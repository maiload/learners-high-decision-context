package com.example.opa.policydecisionlog.command.app.dto;

import java.time.OffsetDateTime;

public record InfrastructureFailureEvent(
        String topic,
        int partition,
        long offset,
        String key,
        String value,
        String errorMessage,
        OffsetDateTime failedAt
) {
    public static InfrastructureFailureEvent of(
            String topic,
            int partition,
            long offset,
            String key,
            String value,
            String errorMessage
    ) {
        return new InfrastructureFailureEvent(
                topic, partition, offset, key, value, errorMessage, OffsetDateTime.now()
        );
    }
}
