package com.example.opa.policydecisionlog.command.infra.kafka;

import com.example.opa.policydecisionlog.command.api.dto.DecisionLogIngestRequest;
import com.example.opa.policydecisionlog.command.api.mapper.RequestToCommandMapper;
import com.example.opa.policydecisionlog.command.app.DecisionLogCommandService;
import com.example.opa.policydecisionlog.command.app.dto.DecisionLogIngestCommand;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.json.JsonMapper;

import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class DecisionLogConsumer {

    private final DecisionLogCommandService commandService;
    private final RequestToCommandMapper mapper;
    private final JsonMapper jsonMapper;

    @KafkaListener(topics = "${opa.kafka.topic:decision-logs}")
    public void consume(List<String> messages, Acknowledgment ack) {
        log.debug("Received {} message(s) from Kafka", messages.size());

        List<DecisionLogIngestCommand> commands = messages.stream()
                .flatMap(message -> {
                    List<DecisionLogIngestRequest> requests = jsonMapper.readValue(
                            message, new TypeReference<>() {}
                    );
                    return requests.stream();
                })
                .map(mapper::toCommand)
                .toList();

        commandService.ingestLogs(commands);
        ack.acknowledge();

        log.info("Saved {} decision log(s)", commands.size());
    }
}
