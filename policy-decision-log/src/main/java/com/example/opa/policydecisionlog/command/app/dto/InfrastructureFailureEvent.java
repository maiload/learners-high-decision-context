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
    public static InfrastructureFailureEvent fromRecord(ConsumerRecord<?, ?> consumerRecord, Exception exception) {
        return new InfrastructureFailureEvent(
                consumerRecord.topic(),
                consumerRecord.partition(),
                consumerRecord.offset(),
                consumerRecord.key() != null ? consumerRecord.key().toString() : null,
                consumerRecord.value() != null ? consumerRecord.value().toString() : null,
                exception.getMessage(),
                OffsetDateTime.now()
        );
    }
}
