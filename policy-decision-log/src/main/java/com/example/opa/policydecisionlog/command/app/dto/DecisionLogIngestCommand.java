package com.example.opa.policydecisionlog.command.app.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import tools.jackson.databind.JsonNode;

import java.time.OffsetDateTime;
import java.util.UUID;

@JsonIgnoreProperties(ignoreUnknown = true)
public record DecisionLogIngestCommand(
        @JsonProperty("decision_id") UUID decisionId,
        OffsetDateTime timestamp,
        String path,
        @JsonProperty("requested_by") String requestedBy,
        @JsonProperty("req_id") Long reqId,
        Labels labels,
        JsonNode bundles,
        JsonNode input,
        JsonNode result,
        @JsonIgnore JsonNode raw
) {
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Labels(
            UUID id,
            String version
    ) {
    }

    public UUID opaInstanceId() {
        return labels != null ? labels.id() : null;
    }

    public String opaVersion() {
        return labels != null ? labels.version() : null;
    }

    public DecisionLogIngestCommand withRaw(JsonNode rawJson) {
        return new DecisionLogIngestCommand(
                decisionId, timestamp, path, requestedBy, reqId,
                labels, bundles, input, result, rawJson
        );
    }
}
