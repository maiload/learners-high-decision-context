package com.example.opa.policydecisionlog.shared.exception;

import lombok.Getter;

import java.util.UUID;

@Getter
public class DecisionNotFoundException extends RuntimeException {

    private final UUID decisionId;

    public DecisionNotFoundException(UUID decisionId) {
        super("Decision not found: " + decisionId);
        this.decisionId = decisionId;
    }

}
