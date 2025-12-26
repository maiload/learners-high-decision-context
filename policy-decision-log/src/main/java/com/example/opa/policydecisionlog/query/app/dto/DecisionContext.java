package com.example.opa.policydecisionlog.query.app.dto;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public record DecisionContext(
        Request request,
        RequestInfo requestInfo,
        OpaInfo opa,
        Decision decision,
        String rawLink
) {

    public record Request(
            UUID decisionId,
            OffsetDateTime decidedAt,
            boolean overallAllow,
            String path,
            String service
    ) {}

    public record RequestInfo(
            String requestedBy,
            Long reqId
    ) {}

    public record OpaInfo(
            UUID instanceId,
            String version,
            Map<String, Object> bundles
    ) {}

    public record Decision(
            List<Reason> reasons,
            List<PolicyResult> policies
    ) {}

    public record Reason(
            Rule rule,
            String message,
            int weight
    ) {}

    public record Rule(
            String policy,
            String expression,
            String name,
            String type
    ) {}

    public record PolicyResult(
            String policy,
            boolean allow,
            int violationsCount
    ) {}
}
