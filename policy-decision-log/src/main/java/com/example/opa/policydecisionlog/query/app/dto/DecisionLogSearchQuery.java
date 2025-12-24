package com.example.opa.policydecisionlog.query.app.dto;

import java.time.OffsetDateTime;

public record DecisionLogSearchQuery(
        OffsetDateTime from,
        OffsetDateTime to,
        Boolean allow,
        String service,
        String path,
        int limit,
        OffsetDateTime cursor
) {
    public static DecisionLogSearchQuery of(
            OffsetDateTime from,
            OffsetDateTime to,
            Boolean allow,
            String service,
            String path,
            int limit,
            OffsetDateTime cursor
    ) {
        return new DecisionLogSearchQuery(from, to, allow, service, path, limit, cursor);
    }
}
