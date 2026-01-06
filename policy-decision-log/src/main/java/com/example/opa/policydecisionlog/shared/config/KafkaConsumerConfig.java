package com.example.opa.policydecisionlog.shared.config;

import com.example.opa.policydecisionlog.command.app.port.InfrastructureFailureWriter;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.common.TopicPartition;
import org.jspecify.annotations.NullMarked;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.kafka.autoconfigure.KafkaProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.listener.BatchListenerFailedException;
import org.springframework.kafka.listener.CommonErrorHandler;
import org.springframework.kafka.listener.ContainerProperties;
import org.springframework.kafka.listener.DeadLetterPublishingRecoverer;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.kafka.listener.MessageListenerContainer;

@Slf4j
@Configuration
public class KafkaConsumerConfig {

    private final KafkaProperties kafkaProperties;
    private final KafkaCustomProperties customProperties;
    private final KafkaTemplate<String, String> dlqKafkaTemplate;

    public KafkaConsumerConfig(
            KafkaProperties kafkaProperties,
            KafkaCustomProperties customProperties,
            @Qualifier("dlqKafkaTemplate") KafkaTemplate<String, String> dlqKafkaTemplate
    ) {
        this.kafkaProperties = kafkaProperties;
        this.customProperties = customProperties;
        this.dlqKafkaTemplate = dlqKafkaTemplate;
    }

    @Bean
    public ConsumerFactory<String, String> consumerFactory() {
        return new DefaultKafkaConsumerFactory<>(kafkaProperties.buildConsumerProperties());
    }

    @Bean
    public DefaultErrorHandler dlqErrorHandler() {
        DeadLetterPublishingRecoverer dlqRecoverer = new DeadLetterPublishingRecoverer(
                dlqKafkaTemplate,
                (rec, ex) -> new TopicPartition(customProperties.dlqTopic(), rec.partition())
        );

        DefaultErrorHandler handler = new DefaultErrorHandler(
                dlqRecoverer,
                customProperties.consumerBackoff().toExponentialBackOff()
        );
        handler.setRetryListeners((rec, ex, deliveryAttempt) ->
                log.warn("DLQ retry attempt {} for record: topic={}, partition={}, offset={}",
                        deliveryAttempt, rec.topic(), rec.partition(), rec.offset())
        );

        return handler;
    }

    @Bean
    public DefaultErrorHandler infraErrorHandler(InfrastructureFailureWriter failureWriter) {
        DefaultErrorHandler handler = new DefaultErrorHandler(
                failureWriter::write,
                customProperties.consumerBackoff().toExponentialBackOff()
        );
        handler.setRetryListeners((rec, ex, deliveryAttempt) ->
                log.warn("Infra retry attempt {} for record: topic={}, partition={}, offset={}",
                        deliveryAttempt, rec.topic(), rec.partition(), rec.offset())
        );

        return handler;
    }

    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, String> kafkaListenerContainerFactory(
            DefaultErrorHandler dlqErrorHandler,
            DefaultErrorHandler infraErrorHandler
    ) {
        ConcurrentKafkaListenerContainerFactory<String, String> factory =
                new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(consumerFactory());
        factory.setBatchListener(true);
        factory.getContainerProperties().setAckMode(ContainerProperties.AckMode.MANUAL);
        factory.setCommonErrorHandler(compositeErrorHandler(dlqErrorHandler, infraErrorHandler));
        return factory;
    }

    private CommonErrorHandler compositeErrorHandler(
            DefaultErrorHandler dlqHandler,
            DefaultErrorHandler infraHandler
    ) {
        return new CommonErrorHandler() {

            @Override
            @NullMarked
            public void handleBatch(Exception exception, ConsumerRecords<?, ?> records,
                                    Consumer<?, ?> consumer, MessageListenerContainer container,
                                    Runnable invokeListener) {
                if (isBatchListenerFailedException(exception)) {
                    dlqHandler.handleBatch(exception, records, consumer, container, invokeListener);
                } else {
                    infraHandler.handleBatch(exception, records, consumer, container, invokeListener);
                }
            }

            private boolean isBatchListenerFailedException(Exception exception) {
                return exception instanceof BatchListenerFailedException
                        || (exception.getCause() instanceof BatchListenerFailedException);
            }
        };
    }
}
