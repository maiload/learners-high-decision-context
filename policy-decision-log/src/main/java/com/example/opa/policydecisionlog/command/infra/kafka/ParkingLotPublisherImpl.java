package com.example.opa.policydecisionlog.command.infra.kafka;

import com.example.opa.policydecisionlog.command.app.dto.DecisionLogIngestCommand;
import com.example.opa.policydecisionlog.command.app.port.ParkingLotPublisher;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;
import tools.jackson.databind.json.JsonMapper;

import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class ParkingLotPublisherImpl implements ParkingLotPublisher {

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final JsonMapper jsonMapper;

    @Value("${opa.kafka.parking-lot-topic:decision-logs.parking-lot}")
    private String topic;

    @Override
    public void publish(List<DecisionLogIngestCommand> commands) {
        String payload = jsonMapper.writeValueAsString(commands);
        kafkaTemplate.send(topic, payload)
                .whenComplete((result, ex) -> {
                    if (ex != null) {
                        log.error("Failed to send to parking lot", ex);
                    } else {
                        log.info("Sent {} command(s) to parking lot: partition={}, offset={}",
                                commands.size(),
                                result.getRecordMetadata().partition(),
                                result.getRecordMetadata().offset());
                    }
                });
    }
}
