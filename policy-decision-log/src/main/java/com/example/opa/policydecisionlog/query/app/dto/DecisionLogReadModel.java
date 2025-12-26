package com.example.opa.policydecisionlog.query.app.dto;

import java.time.OffsetDateTime;
import java.util.Map;
import java.util.UUID;

public record DecisionLogReadModel(
        UUID decisionId,
        OffsetDateTime timestamp,
        String path,
        boolean overallAllow,
        String requestedBy,
        Long reqId,
        UUID opaInstanceId,
        String opaVersion,
        String service,
        Map<String, Object> bundles,
        Map<String, Object> raw,
        OffsetDateTime createdAt
) {
}
