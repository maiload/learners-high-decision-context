package com.example.opa.policydecisionlog.shared.mapper;

import com.example.opa.policydecisionlog.command.api.dto.DecisionLogIngestRequest;
import com.example.opa.policydecisionlog.command.app.dto.IngestDecisionLogCommand;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

@Component
@RequiredArgsConstructor
public class ApiToCommandMapper {

    private final JsonMapper jsonMapper;

    public IngestDecisionLogCommand toIngestDecisionLogCommand(DecisionLogIngestRequest request) {
        DecisionLogIngestRequest.LabelsDto labels = request.labels();
        JsonNode raw = jsonMapper.valueToTree(request);

        return IngestDecisionLogCommand.of(
                request.decisionId(),
                request.timestamp(),
                request.path(),
                request.requestedBy(),
                request.reqId(),
                labels != null ? labels.id() : null,
                labels != null ? labels.version() : null,
                request.bundles(),
                request.input(),
                request.result(),
                raw
        );
    }
}
