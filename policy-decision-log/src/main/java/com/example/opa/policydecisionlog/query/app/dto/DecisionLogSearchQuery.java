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
}
