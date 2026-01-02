package com.example.opa.policydecisionlog.command.app.dto;

import java.time.OffsetDateTime;

public record DecisionLogFailureEvent(
        String rawMessage,
        String errorType,
        String errorMessage,
        String originalTopic,
        Integer partition,
        Long offset,
        OffsetDateTime processedAt
) {
    public static DecisionLogFailureEvent of(String rawMessage, Exception exception, String topic, Integer partition, Long offset) {
        return new DecisionLogFailureEvent(
                rawMessage,
                exception.getClass().getSimpleName(),
                exception.getMessage(),
                topic,
                partition,
                offset,
                OffsetDateTime.now()
        );
    }

    public static DecisionLogFailureEvent fromCommand(DecisionLogIngestCommand command, String originalTopic) {
        return new DecisionLogFailureEvent(
                command.toString(),
                "PersistenceFailure",
                "Failed to persist after bisect",
                originalTopic,
                null,
                null,
                OffsetDateTime.now()
        );
    }
}
