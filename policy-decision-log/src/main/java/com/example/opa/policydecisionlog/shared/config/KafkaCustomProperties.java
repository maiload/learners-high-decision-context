package com.example.opa.policydecisionlog.shared.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.util.backoff.ExponentialBackOff;

@ConfigurationProperties(prefix = "opa.kafka")
public record KafkaCustomProperties(
        String topic,
        String dlqTopic,
        String parkingLotTopic,
        String parkingDlqTopic,
        ProducerSettings fastProducer,
        ProducerSettings parkingProducer,
        ProducerSettings dlqProducer,
        ParkingRecoverySettings parkingRecovery,
        ConsumerBackoff consumerBackoff
) {
    public record ProducerSettings(
            int retries,
            int deliveryTimeoutMs,  // 한 레코드 당 최대 시간
            int requestTimeoutMs,   // 브로커 1회 요청 대기 시간
            int maxBlockMs
    ) {
        private static final int GET_TIMEOUT_MARGIN_MS = 1000;

        public ProducerSettings {
            if (retries <= 0) retries = 3;
            if (deliveryTimeoutMs <= 0) deliveryTimeoutMs = 30000;
            if (requestTimeoutMs <= 0) requestTimeoutMs = 10000;
            if (maxBlockMs <= 0) maxBlockMs = 5000;
        }

        public int getTimeoutMs() {
            return deliveryTimeoutMs - GET_TIMEOUT_MARGIN_MS;
        }
    }

    public record ParkingRecoverySettings(
            int maxRetryAttempts,
            long initialBackoffMs,
            double backoffMultiplier,
            long maxBackoffMs
    ) {
        public ParkingRecoverySettings {
            if (maxRetryAttempts <= 0) maxRetryAttempts = 5;
            if (initialBackoffMs <= 0) initialBackoffMs = 60000;  // 1분
            if (backoffMultiplier <= 0) backoffMultiplier = 2.0;
            if (maxBackoffMs <= 0) maxBackoffMs = 3600000;  // 1시간
        }

        public long calculateNextBackoff(int attempt) {
            long backoff = (long) (initialBackoffMs * Math.pow(backoffMultiplier, attempt));
            return Math.min(backoff, maxBackoffMs);
        }

        public boolean isMaxRetryExceeded(int attempt) {
            return attempt >= maxRetryAttempts;
        }
    }

    public record ConsumerBackoff(
            long initialIntervalMs,
            double multiplier,
            long maxIntervalMs,
            long maxElapsedTimeMs
    ) {
        public ConsumerBackoff {
            if (initialIntervalMs <= 0) initialIntervalMs = 1000;
            if (multiplier <= 0) multiplier = 2.0;
            if (maxIntervalMs <= 0) maxIntervalMs = 10000;
            if (maxElapsedTimeMs <= 0) maxElapsedTimeMs = 30000;
        }

        public ExponentialBackOff toExponentialBackOff() {
            ExponentialBackOff backOff = new ExponentialBackOff();
            backOff.setInitialInterval(initialIntervalMs);
            backOff.setMultiplier(multiplier);
            backOff.setMaxInterval(maxIntervalMs);
            backOff.setMaxElapsedTime(maxElapsedTimeMs);
            return backOff;
        }
    }
}
