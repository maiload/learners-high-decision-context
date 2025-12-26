package com.example.opa.policydecisionlog.query.app.extractor;

import com.example.opa.policydecisionlog.query.app.dto.DecisionContext.PolicyResult;
import com.example.opa.policydecisionlog.query.app.dto.DecisionContext.Reason;
import tools.jackson.databind.JsonNode;

import java.util.List;

public interface DecisionExtractor {

    boolean supports(String service);

    List<Reason> extractReasons(JsonNode raw);

    List<PolicyResult> extractPolicies(JsonNode raw);
}
