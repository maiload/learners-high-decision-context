package com.example.opa.policydecisionlog.command.infra.kafka;

import com.example.opa.policydecisionlog.command.app.PersistDecisionLogUseCase;
import com.example.opa.policydecisionlog.command.app.dto.DecisionLogIngestCommand;
import com.example.opa.policydecisionlog.command.app.dto.PersistResult;
import com.example.opa.policydecisionlog.command.app.error.DataErrorException;
import com.example.opa.policydecisionlog.command.infra.kafka.exception.ConsumerProcessingException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.listener.BatchListenerFailedException;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;
import tools.jackson.databind.json.JsonMapper;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class DecisionLogConsumer {

    private final PersistDecisionLogUseCase persistDecisionLogUseCase;
    private final JsonMapper jsonMapper;

    @KafkaListener(topics = "${opa.kafka.topic:decision-logs}")
    public void consume(List<ConsumerRecord<String, String>> records, Acknowledgment ack) {
        log.debug("Received {} record(s) from Kafka", records.size());

        List<DecisionLogIngestCommand> commands = parseRecords(records);

        if (!commands.isEmpty()) {
            persistCommands(commands, records);
        }

        ack.acknowledge();
        log.info("Processed {} record(s), saved {} decision log(s)", records.size(), commands.size());
    }

    private List<DecisionLogIngestCommand> parseRecords(List<ConsumerRecord<String, String>> records) {
        List<DecisionLogIngestCommand> commands = new ArrayList<>();

        for (ConsumerRecord<String, String> consumerRecord : records) {
            try {
                DecisionLogIngestCommand command = jsonMapper.readValue(
                        consumerRecord.value(), DecisionLogIngestCommand.class
                );
                commands.add(command);
            } catch (Exception e) {
                log.error("Failed to parse message: partition={}, offset={}",
                        consumerRecord.partition(), consumerRecord.offset(), e);
                throw new BatchListenerFailedException("Parse failed", e, consumerRecord);
            }
        }

        return commands;
    }

    private void persistCommands(List<DecisionLogIngestCommand> commands,
                                  List<ConsumerRecord<String, String>> records) {
        try {
            PersistResult result = persistDecisionLogUseCase.execute(commands);
            if (result == PersistResult.FAILED) {
                throw new ConsumerProcessingException("Persist failed after retries and parking failed");
            }
        } catch (DataErrorException e) {
            ConsumerRecord<String, String> failedRecord = records.get(e.getFailedIndex());
            log.warn("Data error detected, sending to DLQ: partition={}, offset={}",
                    failedRecord.partition(), failedRecord.offset());
            throw new BatchListenerFailedException("Data error", e, failedRecord);
        }
    }
}
