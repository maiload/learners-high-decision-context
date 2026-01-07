package com.example.opa.policydecisionlog.command.app.port;

import com.example.opa.policydecisionlog.command.app.dto.InfrastructureFailureEvent;

public interface InfrastructureFailureWriter {
    void write(InfrastructureFailureEvent event);
}
