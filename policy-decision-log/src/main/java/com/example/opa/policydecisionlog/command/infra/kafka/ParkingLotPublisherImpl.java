package com.example.opa.policydecisionlog.command.infra.kafka;

import com.example.opa.policydecisionlog.command.app.dto.DecisionLogIngestCommand;
import com.example.opa.policydecisionlog.command.app.port.ParkingLotPublisher;
import com.example.opa.policydecisionlog.command.infra.kafka.exception.ParkingLotPublishException;
import com.example.opa.policydecisionlog.shared.config.KafkaCustomProperties;
import com.example.opa.policydecisionlog.shared.exception.MissingDecisionIdException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;
import tools.jackson.databind.json.JsonMapper;

import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

@Slf4j
@Component
public class ParkingLotPublisherImpl implements ParkingLotPublisher {

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final JsonMapper jsonMapper;
    private final KafkaCustomProperties properties;

    public ParkingLotPublisherImpl(
            @Qualifier("parkingKafkaTemplate") KafkaTemplate<String, String> kafkaTemplate,
            JsonMapper jsonMapper,
            KafkaCustomProperties properties
    ) {
        this.kafkaTemplate = kafkaTemplate;
        this.jsonMapper = jsonMapper;
        this.properties = properties;
    }

    @Override
    public void publish(List<DecisionLogIngestCommand> commands) {
        for (DecisionLogIngestCommand command : commands) {
            if (command.decisionId() == null) {
                throw new MissingDecisionIdException();
            }
            try {
                String key = command.decisionId().toString();
                String payload = jsonMapper.writeValueAsString(command);
                var result = kafkaTemplate.send(properties.parkingLotTopic(), key, payload)
                        .get(properties.parkingProducer().getTimeoutMs(), TimeUnit.MILLISECONDS);
                log.info("Sent to parking lot: topic={}, partition={}, offset={}, decisionId={}",
                        properties.parkingLotTopic(), result.getRecordMetadata().partition(),
                        result.getRecordMetadata().offset(), key);
            } catch (ExecutionException | InterruptedException | TimeoutException e) {
                log.error("Failed to send to parking lot: topic={}, decisionId={}", properties.parkingLotTopic(), command.decisionId(), e);
                if (e instanceof InterruptedException) {
                    Thread.currentThread().interrupt();
                }
                throw new ParkingLotPublishException(properties.parkingLotTopic(), e);
            }
        }
        log.info("Sent {} command(s) to parking lot", commands.size());
    }
}
