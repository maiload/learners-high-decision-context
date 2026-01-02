package com.example.opa.policydecisionlog.command.app.port;

import java.util.List;
import java.util.Map;

public interface DecisionLogEventPublisher {

    void publish(List<Map<String, Object>> requests);
}
