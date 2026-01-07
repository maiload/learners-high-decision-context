package com.example.opa.policydecisionlog.command.infra.kafka;

import com.example.opa.policydecisionlog.command.app.dto.DecisionLogIngestCommand;
import com.example.opa.policydecisionlog.command.app.port.ParkingLotPublisher;
import com.example.opa.policydecisionlog.command.infra.kafka.exception.KafkaInfraException;
import com.example.opa.policydecisionlog.shared.config.KafkaCustomProperties;
import com.example.opa.policydecisionlog.shared.exception.MissingDecisionIdException;
import com.example.opa.policydecisionlog.shared.kafka.ParkingHeaders;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.stereotype.Component;
import tools.jackson.databind.json.JsonMapper;

import java.time.Instant;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

@Slf4j
@Component
public class ParkingLotPublisherImpl implements ParkingLotPublisher {

    private final KafkaTemplate<String, String> parkingKafkaTemplate;
    private final KafkaTemplate<String, String> dlqKafkaTemplate;
    private final JsonMapper jsonMapper;
    private final KafkaCustomProperties properties;

    public ParkingLotPublisherImpl(
            @Qualifier("parkingKafkaTemplate") KafkaTemplate<String, String> parkingKafkaTemplate,
            @Qualifier("dlqKafkaTemplate") KafkaTemplate<String, String> dlqKafkaTemplate,
            JsonMapper jsonMapper,
            KafkaCustomProperties properties
    ) {
        this.parkingKafkaTemplate = parkingKafkaTemplate;
        this.dlqKafkaTemplate = dlqKafkaTemplate;
        this.jsonMapper = jsonMapper;
        this.properties = properties;
    }

    @Override
    public void publish(List<DecisionLogIngestCommand> commands) {
        long nowMillis = Instant.now().toEpochMilli();
        long notBefore = nowMillis + properties.parkingRecovery().initialBackoffMs();

        for (DecisionLogIngestCommand command : commands) {
            if (command.decisionId() == null) {
                throw new MissingDecisionIdException();
            }
            try {
                String key = command.decisionId().toString();
                String payload = jsonMapper.writeValueAsString(command);

                Message<String> message = MessageBuilder.withPayload(payload)
                        .setHeader(KafkaHeaders.TOPIC, properties.parkingLotTopic())
                        .setHeader(KafkaHeaders.KEY, key)
                        .setHeader(ParkingHeaders.RETRY_ATTEMPT, 0)
                        .setHeader(ParkingHeaders.NOT_BEFORE, notBefore)
                        .build();

                var result = parkingKafkaTemplate.send(message)
                        .get(properties.parkingProducer().getTimeoutMs(), TimeUnit.MILLISECONDS);
                log.info("Sent to parking lot: topic={}, partition={}, offset={}, decisionId={}, notBefore={}",
                        properties.parkingLotTopic(), result.getRecordMetadata().partition(),
                        result.getRecordMetadata().offset(), key, notBefore);
            } catch (ExecutionException | InterruptedException | TimeoutException e) {
                log.error("Failed to send to parking lot: topic={}, decisionId={}", properties.parkingLotTopic(), command.decisionId(), e);
                if (e instanceof InterruptedException) {
                    Thread.currentThread().interrupt();
                }
                throw new KafkaInfraException("Failed to publish to parking lot: " + properties.parkingLotTopic(), e);
            }
        }
        log.info("Sent {} command(s) to parking lot", commands.size());
    }

    @Override
    public void retry(DecisionLogIngestCommand command, int attempt) {
        if (command.decisionId() == null) {
            throw new MissingDecisionIdException();
        }

        long nowMillis = Instant.now().toEpochMilli();
        long notBefore = nowMillis + properties.parkingRecovery().calculateNextBackoff(attempt);

        try {
            String key = command.decisionId().toString();
            String payload = jsonMapper.writeValueAsString(command);

            Message<String> message = MessageBuilder.withPayload(payload)
                    .setHeader(KafkaHeaders.TOPIC, properties.parkingLotTopic())
                    .setHeader(KafkaHeaders.KEY, key)
                    .setHeader(ParkingHeaders.RETRY_ATTEMPT, attempt)
                    .setHeader(ParkingHeaders.NOT_BEFORE, notBefore)
                    .build();

            var result = parkingKafkaTemplate.send(message)
                    .get(properties.parkingProducer().getTimeoutMs(), TimeUnit.MILLISECONDS);
            log.info("Republished to parking lot: topic={}, partition={}, offset={}, decisionId={}, attempt={}, notBefore={}",
                    properties.parkingLotTopic(), result.getRecordMetadata().partition(),
                    result.getRecordMetadata().offset(), key, attempt, notBefore);
        } catch (ExecutionException | InterruptedException | TimeoutException e) {
            log.error("Failed to republish to parking lot: topic={}, decisionId={}, attempt={}",
                    properties.parkingLotTopic(), command.decisionId(), attempt, e);
            if (e instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
            throw new KafkaInfraException("Failed to publish to parking lot: " + properties.parkingLotTopic(), e);
        }
    }

    @Override
    public void toDlq(DecisionLogIngestCommand command) {
        if (command.decisionId() == null) {
            throw new MissingDecisionIdException();
        }

        try {
            String key = command.decisionId().toString();
            String payload = jsonMapper.writeValueAsString(command);

            Message<String> message = MessageBuilder.withPayload(payload)
                    .setHeader(KafkaHeaders.TOPIC, properties.dlqTopic())
                    .setHeader(KafkaHeaders.KEY, key)
                    .build();

            var result = dlqKafkaTemplate.send(message)
                    .get(properties.dlqProducer().getTimeoutMs(), TimeUnit.MILLISECONDS);
            log.warn("Sent to DLQ: topic={}, partition={}, offset={}, decisionId={}",
                    properties.dlqTopic(), result.getRecordMetadata().partition(),
                    result.getRecordMetadata().offset(), key);
        } catch (ExecutionException | InterruptedException | TimeoutException e) {
            log.error("Failed to send to DLQ: topic={}, decisionId={}",
                    properties.dlqTopic(), command.decisionId(), e);
            if (e instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
            throw new KafkaInfraException("Failed to publish to DLQ: " + properties.dlqTopic(), e);
        }
    }

    @Override
    public void toParkingDlq(DecisionLogIngestCommand command) {
        if (command.decisionId() == null) {
            throw new MissingDecisionIdException();
        }

        try {
            String key = command.decisionId().toString();
            String payload = jsonMapper.writeValueAsString(command);

            Message<String> message = MessageBuilder.withPayload(payload)
                    .setHeader(KafkaHeaders.TOPIC, properties.parkingDlqTopic())
                    .setHeader(KafkaHeaders.KEY, key)
                    .build();

            var result = dlqKafkaTemplate.send(message)
                    .get(properties.dlqProducer().getTimeoutMs(), TimeUnit.MILLISECONDS);
            log.warn("Sent to parking DLQ: topic={}, partition={}, offset={}, decisionId={}",
                    properties.parkingDlqTopic(), result.getRecordMetadata().partition(),
                    result.getRecordMetadata().offset(), key);
        } catch (ExecutionException | InterruptedException | TimeoutException e) {
            log.error("Failed to send to parking DLQ: topic={}, decisionId={}",
                    properties.parkingDlqTopic(), command.decisionId(), e);
            if (e instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
            throw new KafkaInfraException("Failed to publish to parking DLQ: " + properties.parkingDlqTopic(), e);
        }
    }
}
