package com.example.opa.policydecisionlog.command.infra.kafka;

import com.example.opa.policydecisionlog.command.app.port.DecisionLogEventPublisher;
import com.example.opa.policydecisionlog.command.infra.kafka.exception.DecisionLogPublishException;
import com.example.opa.policydecisionlog.shared.config.KafkaCustomProperties;
import com.example.opa.policydecisionlog.shared.exception.MissingDecisionIdException;
import com.example.opa.policydecisionlog.shared.metrics.DecisionLogMetrics;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;
import tools.jackson.databind.json.JsonMapper;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

@Slf4j
@Component
public class DecisionLogEventPublisherImpl implements DecisionLogEventPublisher {

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final JsonMapper jsonMapper;
    private final KafkaCustomProperties properties;
    private final DecisionLogMetrics metrics;

    public DecisionLogEventPublisherImpl(
            @Qualifier("fastKafkaTemplate") KafkaTemplate<String, String> kafkaTemplate,
            JsonMapper jsonMapper,
            KafkaCustomProperties properties,
            DecisionLogMetrics metrics
    ) {
        this.kafkaTemplate = kafkaTemplate;
        this.jsonMapper = jsonMapper;
        this.properties = properties;
        this.metrics = metrics;
    }

    @Override
    public void publish(List<Map<String, Object>> requests) {
        for (Map<String, Object> request : requests) {
            try {
                String key = extractDecisionId(request);
                String payload = jsonMapper.writeValueAsString(request);
                var result = kafkaTemplate.send(properties.topic(), key, payload)
                        .get(properties.fastProducer().getTimeoutMs(), TimeUnit.MILLISECONDS);
                metrics.recordPublish(true);
                log.info("Sent decision log: topic={}, partition={}, offset={}, decisionId={}",
                        properties.topic(), result.getRecordMetadata().partition(),
                        result.getRecordMetadata().offset(), key);
            } catch (ExecutionException | InterruptedException | TimeoutException e) {
                metrics.recordPublish(false);
                log.error("Failed to send decision log: topic={}", properties.topic(), e);
                if (e instanceof InterruptedException) {
                    Thread.currentThread().interrupt();
                }
                throw new DecisionLogPublishException(properties.topic(), e);
            }
        }
        log.info("Published {} decision log(s) to Kafka", requests.size());
    }

    private String extractDecisionId(Map<String, Object> request) {
        Object decisionId = request.get("decision_id");
        if (decisionId == null) {
            throw new MissingDecisionIdException();
        }
        return decisionId.toString();
    }
}
