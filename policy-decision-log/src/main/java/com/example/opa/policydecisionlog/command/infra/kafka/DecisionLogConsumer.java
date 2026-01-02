package com.example.opa.policydecisionlog.command.infra.kafka;

import com.example.opa.policydecisionlog.command.app.PersistDecisionLogUseCase;
import com.example.opa.policydecisionlog.command.app.dto.DecisionLogIngestCommand;
import com.example.opa.policydecisionlog.command.app.dto.DecisionLogFailureEvent;
import com.example.opa.policydecisionlog.command.app.port.DlqPublisher;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.json.JsonMapper;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class DecisionLogConsumer {

    private final PersistDecisionLogUseCase persistDecisionLogUseCase;
    private final JsonMapper jsonMapper;
    private final DlqPublisher dlqPublisher;

    @KafkaListener(topics = "${opa.kafka.topic:decision-logs}")
    public void consume(List<ConsumerRecord<String, String>> records, Acknowledgment ack) {
        log.debug("Received {} record(s) from Kafka", records.size());

        List<DecisionLogIngestCommand> commands = new ArrayList<>();

        for (ConsumerRecord<String, String> record : records) {
            try {
                List<DecisionLogIngestCommand> parsed = jsonMapper.readValue(
                        record.value(), new TypeReference<>() {}
                );
                commands.addAll(parsed);
            } catch (Exception e) {
                log.error("Failed to parse message, sending to DLQ: partition={}, offset={}",
                        record.partition(), record.offset(), e);
                sendToDlq(record, e);
            }
        }

        if (!commands.isEmpty()) {
            persistDecisionLogUseCase.execute(commands);
        }

        ack.acknowledge();
        log.info("Processed {} record(s), saved {} decision log(s)", records.size(), commands.size());
    }

    private void sendToDlq(ConsumerRecord<String, String> failedRecord, Exception e) {
        DecisionLogFailureEvent event = DecisionLogFailureEvent.of(
                failedRecord.value(),
                e,
                failedRecord.topic(),
                failedRecord.partition(),
                failedRecord.offset()
        );
        dlqPublisher.publish(event);
    }
}
