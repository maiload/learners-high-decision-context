package com.example.opa.policydecisionlog.query.infra.model;

import java.time.OffsetDateTime;
import java.util.Map;
import java.util.UUID;

public record DecisionLogRow(
        Long id,
        UUID decisionId,
        OffsetDateTime ts,
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
