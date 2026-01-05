package com.example.opa.policydecisionlog.shared.config;

import org.apache.kafka.clients.producer.ProducerConfig;
import org.springframework.boot.kafka.autoconfigure.KafkaProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;

import java.util.HashMap;
import java.util.Map;

@Configuration
@EnableConfigurationProperties(KafkaCustomProperties.class)
public class KafkaProducerConfig {

    private final KafkaProperties kafkaProperties;
    private final KafkaCustomProperties customProperties;

    public KafkaProducerConfig(KafkaProperties kafkaProperties, KafkaCustomProperties customProperties) {
        this.kafkaProperties = kafkaProperties;
        this.customProperties = customProperties;
    }

    @Bean
    public ProducerFactory<String, String> fastProducerFactory() {
        return new DefaultKafkaProducerFactory<>(fastProducerConfigs());
    }

    @Bean
    public Map<String, Object> fastProducerConfigs() {
        var settings = customProperties.fastProducer();
        return buildProducerConfigs(settings);
    }

    @Bean
    public KafkaTemplate<String, String> fastKafkaTemplate() {
        return new KafkaTemplate<>(fastProducerFactory());
    }

    @Bean
    public ProducerFactory<String, String> parkingProducerFactory() {
        return new DefaultKafkaProducerFactory<>(parkingProducerConfigs());
    }

    @Bean
    public Map<String, Object> parkingProducerConfigs() {
        var settings = customProperties.parkingProducer();
        return buildProducerConfigs(settings);
    }

    @Bean
    public KafkaTemplate<String, String> parkingKafkaTemplate() {
        return new KafkaTemplate<>(parkingProducerFactory());
    }

    @Bean
    public ProducerFactory<String, String> dlqProducerFactory() {
        return new DefaultKafkaProducerFactory<>(dlqProducerConfigs());
    }

    @Bean
    public Map<String, Object> dlqProducerConfigs() {
        var settings = customProperties.dlqProducer();
        return buildProducerConfigs(settings);
    }

    @Bean
    public KafkaTemplate<String, String> dlqKafkaTemplate() {
        return new KafkaTemplate<>(dlqProducerFactory());
    }

    private Map<String, Object> buildProducerConfigs(KafkaCustomProperties.ProducerSettings settings) {
        Map<String, Object> props = new HashMap<>(kafkaProperties.buildProducerProperties());
        props.put(ProducerConfig.RETRIES_CONFIG, settings.retries());
        props.put(ProducerConfig.DELIVERY_TIMEOUT_MS_CONFIG, settings.deliveryTimeoutMs());
        props.put(ProducerConfig.REQUEST_TIMEOUT_MS_CONFIG, settings.requestTimeoutMs());
        props.put(ProducerConfig.MAX_BLOCK_MS_CONFIG, settings.maxBlockMs());
        return props;
    }
}
