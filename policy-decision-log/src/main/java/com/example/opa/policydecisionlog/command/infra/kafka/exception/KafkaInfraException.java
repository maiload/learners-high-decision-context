package com.example.opa.policydecisionlog.command.infra.kafka.exception;

public class KafkaInfraException extends RuntimeException {

    public KafkaInfraException(String message) {
        super(message);
    }

    public KafkaInfraException(String message, Throwable cause) {
        super(message, cause);
    }
}
