package com.example.opa.policydecisionlog.command.infra.kafka;

import com.example.opa.policydecisionlog.command.app.dto.DecisionLogFailureEvent;
import com.example.opa.policydecisionlog.command.app.dto.DecisionLogIngestCommand;
import com.example.opa.policydecisionlog.command.app.port.DlqPublisher;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;
import tools.jackson.databind.json.JsonMapper;

@Slf4j
@Component
@RequiredArgsConstructor
public class DlqPublisherImpl implements DlqPublisher {

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final JsonMapper jsonMapper;

    @Value("${opa.kafka.dlq-topic:decision-logs.dlq}")
    private String dlqTopic;

    @Value("${opa.kafka.topic:decision-logs}")
    private String originalTopic;

    @Override
    public void publish(DecisionLogIngestCommand command) {
        DecisionLogFailureEvent event = DecisionLogFailureEvent.fromCommand(command, originalTopic);
        String payload = jsonMapper.writeValueAsString(event);

        kafkaTemplate.send(dlqTopic, payload)
                .whenComplete((result, ex) -> {
                    if (ex != null) {
                        log.error("Failed to send message to DLQ", ex);
                    } else {
                        log.warn("Sent failed message to DLQ: topic={}, errorType={}",
                                dlqTopic, event.errorType());
                    }
                });
    }

    @Override
    public void publish(DecisionLogFailureEvent event) {
        String payload = jsonMapper.writeValueAsString(event);

        kafkaTemplate.send(dlqTopic, payload)
                .whenComplete((result, ex) -> {
                    if (ex != null) {
                        log.error("Failed to send message to DLQ", ex);
                    } else {
                        log.warn("Sent failed message to DLQ: topic={}, errorType={}",
                                dlqTopic, event.errorType());
                    }
                });
    }
}
