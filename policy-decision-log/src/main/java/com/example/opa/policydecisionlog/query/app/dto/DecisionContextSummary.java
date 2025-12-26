package com.example.opa.policydecisionlog.query.app.dto;

import java.time.OffsetDateTime;
import java.util.UUID;

public record DecisionContextSummary(
        UUID decisionId,
        boolean overallAllow,
        String path,
        String service,
        OffsetDateTime timestamp,
        String requestedBy,
        String contextLink
) {
}
