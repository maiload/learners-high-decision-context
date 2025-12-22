package com.example.opa.policydecisionlog.query.app.dto;

import java.time.OffsetDateTime;
import java.util.UUID;

public record DecisionLogSearchQuery(
        OffsetDateTime from,
        OffsetDateTime to,
        Boolean allow,
        UUID userId,
        UUID realmId,
        String path,
        int limit,
        OffsetDateTime cursor
) {
    public static DecisionLogSearchQuery of(
            OffsetDateTime from,
            OffsetDateTime to,
            Boolean allow,
            UUID userId,
            UUID realmId,
            String path,
            int limit,
            OffsetDateTime cursor
    ) {
        return new DecisionLogSearchQuery(from, to, allow, userId, realmId, path, limit, cursor);
    }
}
