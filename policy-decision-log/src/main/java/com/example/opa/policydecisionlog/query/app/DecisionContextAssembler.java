package com.example.opa.policydecisionlog.query.app;

import com.example.opa.policydecisionlog.query.app.dto.DecisionContext;
import com.example.opa.policydecisionlog.query.app.dto.DecisionContext.*;
import com.example.opa.policydecisionlog.query.app.dto.DecisionContextSummary;
import com.example.opa.policydecisionlog.query.app.dto.DecisionLogReadModel;
import com.example.opa.policydecisionlog.query.app.extractor.DecisionExtractor;
import com.example.opa.policydecisionlog.query.app.extractor.DecisionExtractorRegistry;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

@Component
@RequiredArgsConstructor
public class DecisionContextAssembler {

    private final DecisionExtractorRegistry extractorRegistry;
    private final JsonMapper jsonMapper;

    public DecisionContextSummary assembleSummary(DecisionLogReadModel readModel) {
        return new DecisionContextSummary(
                readModel.decisionId(),
                readModel.overallAllow(),
                readModel.path(),
                readModel.service(),
                readModel.timestamp(),
                readModel.requestedBy(),
                "/decisions/" + readModel.decisionId() + "/context"
        );
    }

    public DecisionContext assemble(DecisionLogReadModel readModel) {
        DecisionExtractor extractor = extractorRegistry.getExtractor(readModel.service());
        JsonNode rawNode = jsonMapper.valueToTree(readModel.raw());

        return new DecisionContext(
                buildRequest(readModel),
                buildRequestInfo(readModel),
                buildOpaInfo(readModel),
                buildDecision(rawNode, extractor),
                "/decisions/" + readModel.decisionId()
        );
    }

    private Request buildRequest(DecisionLogReadModel readModel) {
        return new Request(
                readModel.decisionId(),
                readModel.timestamp(),
                readModel.overallAllow(),
                readModel.path(),
                readModel.service()
        );
    }

    private RequestInfo buildRequestInfo(DecisionLogReadModel readModel) {
        return new RequestInfo(
                readModel.requestedBy(),
                readModel.reqId()
        );
    }

    private OpaInfo buildOpaInfo(DecisionLogReadModel readModel) {
        return new OpaInfo(
                readModel.opaInstanceId(),
                readModel.opaVersion(),
                readModel.bundles()
        );
    }

    private Decision buildDecision(JsonNode raw, DecisionExtractor extractor) {
        return new Decision(
                extractor.extractReasons(raw),
                extractor.extractPolicies(raw)
        );
    }
}
