package com.example.opa.policydecisionlog.command.infra.kafka.exception;

public class ConsumerProcessingException extends RuntimeException {

    public ConsumerProcessingException(String message) {
        super(message);
    }
}
