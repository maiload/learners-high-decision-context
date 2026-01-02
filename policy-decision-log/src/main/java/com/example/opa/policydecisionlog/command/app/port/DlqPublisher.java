package com.example.opa.policydecisionlog.command.app.port;

import com.example.opa.policydecisionlog.command.app.dto.DecisionLogFailureEvent;
import com.example.opa.policydecisionlog.command.app.dto.DecisionLogIngestCommand;

public interface DlqPublisher {

    void publish(DecisionLogIngestCommand command);

    void publish(DecisionLogFailureEvent event);
}
