package com.example.opa.policydecisionlog.command.infra.kafka;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class DecisionLogProducer {

    private final KafkaTemplate<String, String> kafkaTemplate;

    @Value("${opa.kafka.topic:decision-logs}")
    private String topic;

    public void send(String payload) {
        kafkaTemplate.send(topic, payload)
                .whenComplete((result, ex) -> {
                    if (ex != null) {
                        log.error("Failed to send decision log", ex);
                    } else {
                        log.debug("Sent decision log: partition={}, offset={}",
                                result.getRecordMetadata().partition(),
                                result.getRecordMetadata().offset());
                    }
                });
    }
}
