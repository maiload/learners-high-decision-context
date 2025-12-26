package com.example.opa.policydecisionlog.query.fixture;

import com.example.opa.policydecisionlog.query.app.dto.DecisionLogReadModel;

import java.time.OffsetDateTime;
import java.util.Map;
import java.util.UUID;

public class DecisionLogReadModelFixture {

    public static DecisionLogReadModel createDefault() {
        return new DecisionLogReadModel(
                UUID.randomUUID(),
                OffsetDateTime.now(),
                "/cloud_access/policy/main",
                true,
                "user@example.com",
                12345L,
                UUID.randomUUID(),
                "1.0.0",
                "cloud_access",
                Map.of("bundle1", "v1"),
                Map.of("key", "value"),
                OffsetDateTime.now()
        );
    }

    public static DecisionLogReadModel createWithTimestamp(OffsetDateTime ts) {
        return new DecisionLogReadModel(
                UUID.randomUUID(),
                ts,
                "/cloud_access/policy/main",
                true,
                "user@example.com",
                12345L,
                UUID.randomUUID(),
                "1.0.0",
                "cloud_access",
                Map.of("bundle1", "v1"),
                Map.of("key", "value"),
                OffsetDateTime.now()
        );
    }

    public static DecisionLogReadModel createWithDecisionId(UUID decisionId) {
        return new DecisionLogReadModel(
                decisionId,
                OffsetDateTime.now(),
                "/cloud_access/policy/main",
                true,
                "user@example.com",
                12345L,
                UUID.randomUUID(),
                "1.0.0",
                "cloud_access",
                Map.of("bundle1", "v1"),
                Map.of("key", "value"),
                OffsetDateTime.now()
        );
    }
}
