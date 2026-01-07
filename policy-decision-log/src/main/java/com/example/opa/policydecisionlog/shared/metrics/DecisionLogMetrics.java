package com.example.opa.policydecisionlog.shared.metrics;

import io.micrometer.core.instrument.*;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.TimeUnit;

@Component
public class DecisionLogMetrics {

    private static final String PREFIX = "decision_log";

    // Ingest
    private final Counter ingestTotal;

    // Publish
    private final Counter publishSuccess;
    private final Counter publishFailure;

    // Consume
    private final Timer consumeProcessTime;
    private final Counter dbSaveSuccess;
    private final Counter dbSaveFailure;
    private final Counter dbSaveRetry;

    // DLQ / Parking
    private final Counter dlqSent;
    private final Counter parkingDlqSent;
    private final Counter parkingSent;
    private final Counter parkingRecovered;

    // E2E
    private final Timer endToEndLatency;

    public DecisionLogMetrics(MeterRegistry registry) {
        // Ingest
        this.ingestTotal = Counter.builder(PREFIX + ".ingest.total")
                .description("Total ingested decision log requests")
                .register(registry);

        // Publish
        this.publishSuccess = Counter.builder(PREFIX + ".publish.success")
                .description("Successfully published to Kafka")
                .register(registry);

        this.publishFailure = Counter.builder(PREFIX + ".publish.failure")
                .description("Failed to publish to Kafka")
                .register(registry);

        // Consume
        this.consumeProcessTime = Timer.builder(PREFIX + ".consume.process_time")
                .description("Time to process consumed batch")
                .register(registry);

        this.dbSaveSuccess = Counter.builder(PREFIX + ".db.save.success")
                .description("Successful DB saves")
                .register(registry);

        this.dbSaveFailure = Counter.builder(PREFIX + ".db.save.failure")
                .description("Failed DB saves")
                .register(registry);

        this.dbSaveRetry = Counter.builder(PREFIX + ".db.save.retry")
                .description("DB save retry attempts")
                .register(registry);

        // DLQ / Parking
        this.dlqSent = Counter.builder(PREFIX + ".dlq.sent")
                .description("Messages sent to DLQ")
                .register(registry);

        this.parkingDlqSent = Counter.builder(PREFIX + ".parking.dlq.sent")
                .description("Messages sent to parking DLQ (max retry exceeded)")
                .register(registry);

        this.parkingSent = Counter.builder(PREFIX + ".parking.sent")
                .description("Messages sent to parking lot")
                .register(registry);

        this.parkingRecovered = Counter.builder(PREFIX + ".parking.recovered")
                .description("Messages recovered from parking lot")
                .register(registry);

        // E2E
        this.endToEndLatency = Timer.builder(PREFIX + ".e2e.latency")
                .description("End-to-end latency from OPA timestamp to DB save")
                .register(registry);
    }

    public void recordIngest(int count) {
        ingestTotal.increment(count);
    }

    public void recordPublish(boolean success) {
        if (success) {
            publishSuccess.increment();
        } else {
            publishFailure.increment();
        }
    }

    public void recordConsume(Duration processTime) {
        consumeProcessTime.record(processTime);
    }

    public void recordDbSave(boolean success, int count) {
        if (success) {
            dbSaveSuccess.increment(count);
        } else {
            dbSaveFailure.increment(count);
        }
    }

    public void recordEndToEndLatency(Instant opaTimestamp) {
        if (opaTimestamp != null) {
            Duration latency = Duration.between(opaTimestamp, Instant.now());
            endToEndLatency.record(latency.toMillis(), TimeUnit.MILLISECONDS);
        }
    }

    public void recordDbSaveRetry() {
        dbSaveRetry.increment();
    }

    public void recordDlqSent(int count) {
        dlqSent.increment(count);
    }

    public void recordParkingDlqSent(int count) {
        parkingDlqSent.increment(count);
    }

    public void recordParkingSent(int count) {
        parkingSent.increment(count);
    }

    public void recordParkingRecovered(int count) {
        parkingRecovered.increment(count);
    }
}
