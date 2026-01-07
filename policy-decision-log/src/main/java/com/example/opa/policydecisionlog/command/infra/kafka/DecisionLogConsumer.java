package com.example.opa.policydecisionlog.command.infra.kafka;

import com.example.opa.policydecisionlog.command.app.PersistDecisionLogUseCase;
import com.example.opa.policydecisionlog.command.app.dto.DecisionLogIngestCommand;
import com.example.opa.policydecisionlog.command.app.dto.PersistResult;
import com.example.opa.policydecisionlog.command.infra.kafka.exception.KafkaInfraException;
import com.example.opa.policydecisionlog.shared.metrics.DecisionLogMetrics;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.listener.BatchListenerFailedException;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;
import tools.jackson.databind.json.JsonMapper;

import java.time.Duration;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Slf4j
@Component
@RequiredArgsConstructor
public class DecisionLogConsumer {

    private final PersistDecisionLogUseCase persistDecisionLogUseCase;
    private final JsonMapper jsonMapper;
    private final DecisionLogMetrics metrics;

    @KafkaListener(topics = "${opa.kafka.topic:decision-logs}")
    public void consume(List<ConsumerRecord<String, String>> records, Acknowledgment ack) {
        Instant start = Instant.now();
        log.debug("Received {} record(s) from Kafka", records.size());

        List<DecisionLogIngestCommand> commands = parseRecords(records);

        if (!commands.isEmpty()) {
            persistCommands(commands);
            recordE2ELatency(commands);
        }

        ack.acknowledge();
        metrics.recordConsume(Duration.between(start, Instant.now()));
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

    private void persistCommands(List<DecisionLogIngestCommand> commands) {
        PersistResult result = persistDecisionLogUseCase.execute(commands);
        if (result == PersistResult.FAILED) {
            throw new KafkaInfraException("Persist failed after retries and parking failed");
        }
    }

    private void recordE2ELatency(List<DecisionLogIngestCommand> commands) {
        Instant oldest = commands.stream()
                .map(DecisionLogIngestCommand::timestamp)
                .filter(Objects::nonNull)
                .map(OffsetDateTime::toInstant)
                .min(Instant::compareTo)
                .orElse(null);

        metrics.recordEndToEndLatency(oldest);
    }
}
