package com.example.opa.policydecisionlog.command.app.dto;

import tools.jackson.databind.JsonNode;

import java.time.OffsetDateTime;
import java.util.UUID;

public record DecisionLogIngestCommand(
        UUID decisionId,
        OffsetDateTime timestamp,
        String path,
        String requestedBy,
        Long reqId,
        UUID opaInstanceId,
        String opaVersion,
        JsonNode bundles,
        JsonNode input,
        JsonNode result,
        JsonNode raw
) {
    public static DecisionLogIngestCommand of(
            UUID decisionId,
            OffsetDateTime timestamp,
            String path,
            String requestedBy,
            Long reqId,
            UUID opaInstanceId,
            String opaVersion,
            JsonNode bundles,
            JsonNode input,
            JsonNode result,
            JsonNode raw
    ) {
        return new DecisionLogIngestCommand(
                decisionId,
                timestamp,
                path,
                requestedBy,
                reqId,
                opaInstanceId,
                opaVersion,
                bundles,
                input,
                result,
                raw
        );
    }
}
