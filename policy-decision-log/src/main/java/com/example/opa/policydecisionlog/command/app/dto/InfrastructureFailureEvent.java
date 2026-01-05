package com.example.opa.policydecisionlog.command.app.dto;

import org.apache.kafka.clients.consumer.ConsumerRecord;

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
    public static InfrastructureFailureEvent fromRecord(ConsumerRecord<?, ?> record, Exception exception) {
        return new InfrastructureFailureEvent(
                record.topic(),
                record.partition(),
                record.offset(),
                record.key() != null ? record.key().toString() : null,
                record.value() != null ? record.value().toString() : null,
                exception.getMessage(),
                OffsetDateTime.now()
        );
    }
}
