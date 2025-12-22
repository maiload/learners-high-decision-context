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
        UUID realmId,
        UUID userId,
        UUID userPolicyId,
        String osType,
        Map<String, Object> bundles,
        Integer violationCount,
        Map<String, Object> raw,
        OffsetDateTime createdAt
) {
    public static DecisionLogRow of(
            Long id,
            UUID decisionId,
            OffsetDateTime ts,
            String path,
            boolean overallAllow,
            String requestedBy,
            Long reqId,
            UUID opaInstanceId,
            String opaVersion,
            UUID realmId,
            UUID userId,
            UUID userPolicyId,
            String osType,
            Map<String, Object> bundles,
            Integer violationCount,
            Map<String, Object> raw,
            OffsetDateTime createdAt
    ) {
        return new DecisionLogRow(
                id, decisionId, ts, path, overallAllow, requestedBy, reqId,
                opaInstanceId, opaVersion, realmId, userId, userPolicyId,
                osType, bundles, violationCount, raw, createdAt
        );
    }
}
