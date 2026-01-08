package com.example.opa.policydecisionlog.command.infra.kafka;

import com.example.opa.policydecisionlog.command.app.usecase.PersistDecisionLogUseCase;
import com.example.opa.policydecisionlog.command.app.dto.DecisionLogIngestCommand;
import com.example.opa.policydecisionlog.command.app.dto.PersistResult;
import com.example.opa.policydecisionlog.command.app.port.ParkingLotPublisher;
import com.example.opa.policydecisionlog.shared.config.KafkaCustomProperties;
import com.example.opa.policydecisionlog.shared.kafka.ParkingHeaders;
import com.example.opa.policydecisionlog.shared.metrics.DecisionLogMetrics;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;
import tools.jackson.databind.json.JsonMapper;

import java.time.Duration;
import java.time.Instant;

@Slf4j
@Component
@RequiredArgsConstructor
public class ParkingLotConsumer {

    private static final Duration POLL_INTERVAL = Duration.ofSeconds(30);

    private final PersistDecisionLogUseCase persistDecisionLogUseCase;
    private final ParkingLotPublisher parkingLotPublisher;
    private final JsonMapper jsonMapper;
    private final KafkaCustomProperties properties;
    private final DecisionLogMetrics metrics;

    @KafkaListener(
            topics = "${opa.kafka.parking-lot-topic:decision-logs-parking}",
            containerFactory = "parkingKafkaListenerContainerFactory"
    )
    public void consume(
            @Payload String payload,
            @Header(KafkaHeaders.RECEIVED_PARTITION) int partition,
            @Header(KafkaHeaders.OFFSET) long offset,
            @Header(ParkingHeaders.RETRY_ATTEMPT) int attempt,
            @Header(ParkingHeaders.NOT_BEFORE) long notBefore,
            Acknowledgment ack
    ) {
        log.debug("Received parking record: partition={}, offset={}, attempt={}, notBefore={}",
                partition, offset, attempt, notBefore);

        long nowMillis = Instant.now().toEpochMilli();
        if (nowMillis < notBefore) {
            long waitMs = Math.min(notBefore - nowMillis, POLL_INTERVAL.toMillis());
            log.debug("Not ready yet, waiting {}ms: partition={}, offset={}", waitMs, partition, offset);
            ack.nack(Duration.ofMillis(waitMs));
            return;
        }

        DecisionLogIngestCommand command;
        try {
            command = jsonMapper.readValue(payload, DecisionLogIngestCommand.class);
        } catch (RuntimeException e) {
            log.error("Failed to parse parking message, sending to DLQ: partition={}, offset={}", partition, offset, e);
            throw e;
        }

        if (properties.parkingRecovery().isMaxRetryExceeded(attempt)) {
            parkingLotPublisher.toParkingDlq(command);
            metrics.recordParkingDlqSent(1);
            log.warn("Max retry exceeded, sent to parking DLQ: partition={}, offset={}, decisionId={}, attempt={}",
                    partition, offset, command.decisionId(), attempt);
            ack.acknowledge();
            return;
        }

        PersistResult result = persistDecisionLogUseCase.executeRecovery(command, attempt);
        if (result == PersistResult.SUCCESS) {
            metrics.recordParkingRecovered(1);
            log.info("Parking recovery success: partition={}, offset={}, decisionId={}, attempt={}",
                    partition, offset, command.decisionId(), attempt);
        }
        ack.acknowledge();
    }
}
