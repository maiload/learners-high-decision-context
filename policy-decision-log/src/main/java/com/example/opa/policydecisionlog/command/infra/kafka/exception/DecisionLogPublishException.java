package com.example.opa.policydecisionlog.command.infra.kafka.exception;

public class DecisionLogPublishException extends RuntimeException {

    public DecisionLogPublishException(String topic, Throwable cause) {
        super("Failed to publish decision log to: " + topic, cause);
    }
}
