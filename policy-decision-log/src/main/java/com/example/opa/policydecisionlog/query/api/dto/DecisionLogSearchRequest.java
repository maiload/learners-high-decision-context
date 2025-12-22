package com.example.opa.policydecisionlog.query.api.dto;

import java.time.OffsetDateTime;
import java.util.UUID;

public record DecisionLogSearchRequest(
        OffsetDateTime from,
        OffsetDateTime to,
        Boolean allow,
        UUID userId,
        UUID realmId,
        String path,
        Integer limit,
        OffsetDateTime cursor
) {
}
