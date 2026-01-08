package com.example.opa.policydecisionlog.shared.config;

import com.example.opa.policydecisionlog.command.app.dto.InfrastructureFailureEvent;
import com.example.opa.policydecisionlog.command.infra.kafka.exception.KafkaInfraException;
import com.example.opa.policydecisionlog.command.app.port.InfrastructureFailureWriter;
import com.example.opa.policydecisionlog.shared.metrics.DecisionLogMetrics;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerRecord;
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
    public ConsumerFactory<String, String> parkingConsumerFactory() {
        var props = kafkaProperties.buildConsumerProperties();
        props.put("group.id", kafkaProperties.getConsumer().getGroupId() + "-parking");
        return new DefaultKafkaConsumerFactory<>(props);
    }

    @Bean
    public DefaultErrorHandler dlqErrorHandler(DecisionLogMetrics metrics) {
        DeadLetterPublishingRecoverer dlqRecoverer = new DeadLetterPublishingRecoverer(
                dlqKafkaTemplate,
                (rec, ex) -> new TopicPartition(customProperties.dlqTopic(), rec.partition())
        ) {
            @Override
            @NullMarked
            public void accept(ConsumerRecord<?, ?> rec, Exception ex) {
                super.accept(rec, ex);
                metrics.recordDlqSent(1);
                log.info("Sent to DLQ: topic={}, partition={}, offset={}", rec.topic(), rec.partition(), rec.offset());
            }
        };

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
                (rec, ex) -> failureWriter.write(InfrastructureFailureEvent.of(
                        rec.topic(),
                        rec.partition(),
                        rec.offset(),
                        rec.key() != null ? rec.key().toString() : null,
                        rec.value() != null ? rec.value().toString() : null,
                        ex.getMessage()
                )),
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

    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, String> parkingKafkaListenerContainerFactory(
            DefaultErrorHandler dlqErrorHandler,
            DefaultErrorHandler infraErrorHandler
    ) {
        ConcurrentKafkaListenerContainerFactory<String, String> factory =
                new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(parkingConsumerFactory());
        factory.setBatchListener(false);
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
            public void handleBatch(Exception exception, ConsumerRecords<?, ?> consumerRecords,
                                    Consumer<?, ?> consumer, MessageListenerContainer container,
                                    Runnable invokeListener) {
                if (isInfraError(exception)) {
                    infraHandler.handleBatch(exception, consumerRecords, consumer, container, invokeListener);
                } else {
                    dlqHandler.handleBatch(exception, consumerRecords, consumer, container, invokeListener);
                }
            }

            @Override
            @NullMarked
            public boolean handleOne(Exception exception, ConsumerRecord<?, ?> consumerRecord,
                                     Consumer<?, ?> consumer, MessageListenerContainer container) {
                if (isInfraError(exception)) {
                    return infraHandler.handleOne(exception, consumerRecord, consumer, container);
                } else {
                    return dlqHandler.handleOne(exception, consumerRecord, consumer, container);
                }
            }

            private boolean isInfraError(Exception exception) {
                return exception instanceof KafkaInfraException;
            }
        };
    }
}
