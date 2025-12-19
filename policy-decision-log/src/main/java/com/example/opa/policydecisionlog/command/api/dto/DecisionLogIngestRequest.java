package com.example.opa.policydecisionlog.command.api.dto;



import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import tools.jackson.databind.JsonNode;

import java.time.OffsetDateTime;
import java.util.UUID;

@JsonIgnoreProperties(ignoreUnknown = true)
public record DecisionLogIngestRequest(
        @JsonProperty("decision_id") UUID decisionId,
        OffsetDateTime timestamp,
        String path,
        @JsonProperty("requested_by") String requestedBy,
        @JsonProperty("req_id") Long reqId,
        LabelsDto labels,
        JsonNode bundles,
        JsonNode input,
        JsonNode result
) {
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record LabelsDto(
            UUID id,
            String version
    ) {
    }
}
