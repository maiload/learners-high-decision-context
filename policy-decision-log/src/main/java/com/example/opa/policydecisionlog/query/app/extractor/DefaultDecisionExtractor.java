package com.example.opa.policydecisionlog.query.app.extractor;

import com.example.opa.policydecisionlog.query.app.dto.DecisionContext.PolicyResult;
import com.example.opa.policydecisionlog.query.app.dto.DecisionContext.Reason;
import org.springframework.stereotype.Component;
import tools.jackson.databind.JsonNode;

import java.util.List;

@Component
public class DefaultDecisionExtractor implements DecisionExtractor {

    @Override
    public boolean supports(String service) {
        return false;
    }

    @Override
    public List<Reason> extractReasons(JsonNode raw) {
        return List.of();
    }

    @Override
    public List<PolicyResult> extractPolicies(JsonNode raw) {
        return List.of();
    }
}
