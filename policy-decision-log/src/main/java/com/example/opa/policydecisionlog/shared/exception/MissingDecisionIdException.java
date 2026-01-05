package com.example.opa.policydecisionlog.shared.exception;

public class MissingDecisionIdException extends RuntimeException {

    public MissingDecisionIdException() {
        super("decisionId is required but was null");
    }
}
