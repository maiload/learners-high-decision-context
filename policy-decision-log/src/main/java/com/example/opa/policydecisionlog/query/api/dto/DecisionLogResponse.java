package com.example.opa.policydecisionlog.query.api.dto;

import com.example.opa.policydecisionlog.query.infra.model.DecisionLogRow;

import java.time.OffsetDateTime;
import java.util.Map;
import java.util.UUID;

public record DecisionLogResponse(
        UUID decisionId,
        OffsetDateTime timestamp,
        String path,
        boolean allow,
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
    public static DecisionLogResponse from(DecisionLogRow row) {
        return new DecisionLogResponse(
                row.decisionId(),
                row.ts(),
                row.path(),
                row.overallAllow(),
                row.requestedBy(),
                row.reqId(),
                row.opaInstanceId(),
                row.opaVersion(),
                row.realmId(),
                row.userId(),
                row.userPolicyId(),
                row.osType(),
                row.bundles(),
                row.violationCount(),
                row.raw(),
                row.createdAt()
        );
    }
}
