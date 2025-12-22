package com.example.opa.policydecisionlog.query.fixture;

import com.example.opa.policydecisionlog.query.infra.model.DecisionLogRow;

import java.time.OffsetDateTime;
import java.util.Map;
import java.util.UUID;

public class DecisionLogRowFixture {

    public static DecisionLogRow createDefault() {
        return DecisionLogRow.of(
                1L,
                UUID.randomUUID(),
                OffsetDateTime.now(),
                "/policy/main",
                true,
                "user@example.com",
                12345L,
                UUID.randomUUID(),
                "1.0.0",
                UUID.randomUUID(),
                UUID.randomUUID(),
                UUID.randomUUID(),
                "WINDOWS",
                Map.of("bundle1", "v1"),
                0,
                Map.of("key", "value"),
                OffsetDateTime.now()
        );
    }

    public static DecisionLogRow createWithTimestamp(OffsetDateTime ts) {
        return DecisionLogRow.of(
                1L,
                UUID.randomUUID(),
                ts,
                "/policy/main",
                true,
                "user@example.com",
                12345L,
                UUID.randomUUID(),
                "1.0.0",
                UUID.randomUUID(),
                UUID.randomUUID(),
                UUID.randomUUID(),
                "WINDOWS",
                Map.of("bundle1", "v1"),
                0,
                Map.of("key", "value"),
                OffsetDateTime.now()
        );
    }
}
