package com.example.opa.policydecisionlog.command.app.usecase;

import com.example.opa.policydecisionlog.command.app.port.DecisionLogEventPublisher;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class PublishDecisionLogUseCase {

    private final DecisionLogEventPublisher eventPublisher;

    public void execute(List<Map<String, Object>> requests) {
        log.info("Publishing {} decision log(s) to Kafka", requests.size());
        eventPublisher.publish(requests);
    }
}
