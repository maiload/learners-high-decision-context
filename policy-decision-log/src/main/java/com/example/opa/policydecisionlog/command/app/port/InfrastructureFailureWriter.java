package com.example.opa.policydecisionlog.command.app.port;

import org.apache.kafka.clients.consumer.ConsumerRecord;

public interface InfrastructureFailureWriter {
    void write(ConsumerRecord<?, ?> consumerRecord, Exception exception);
}
