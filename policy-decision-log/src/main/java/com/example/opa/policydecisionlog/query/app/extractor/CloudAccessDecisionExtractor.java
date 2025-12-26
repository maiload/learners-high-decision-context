package com.example.opa.policydecisionlog.query.app.extractor;

import com.example.opa.policydecisionlog.query.app.dto.DecisionContext.PolicyResult;
import com.example.opa.policydecisionlog.query.app.dto.DecisionContext.Reason;
import com.example.opa.policydecisionlog.query.app.dto.DecisionContext.Rule;
import org.springframework.stereotype.Component;
import tools.jackson.databind.JsonNode;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.example.opa.policydecisionlog.query.app.extractor.RawJsonKeys.*;

@Component
public class CloudAccessDecisionExtractor implements DecisionExtractor {

    private static final String SERVICE_NAME = "cloud_access";

    @Override
    public boolean supports(String service) {
        return SERVICE_NAME.equals(service);
    }

    @Override
    public List<Reason> extractReasons(JsonNode raw) {
        JsonNode result = raw.path(RESULT);
        if (result.isMissingNode() || result.path(ALLOW).asBoolean()) {
            return List.of();
        }

        Map<String, Integer> weightMap = extractWeight(result);
        List<Reason> reasons = new ArrayList<>();

        for (JsonNode policy : result.path(POLICIES)) {
            String policyName = policy.path(POLICY_NAME).asString(null);
            int weight = weightMap.getOrDefault(policyName, 0);

            JsonNode policyData = policy.path(POLICY_DATA);
            reasons.addAll(extractReasonsFromPolicyData(policyData, policyName, weight));
        }

        reasons.sort(Comparator.comparingInt(Reason::weight).reversed());
        return reasons;
    }

    @Override
    public List<PolicyResult> extractPolicies(JsonNode raw) {
        JsonNode result = raw.path(RESULT);
        if (result.isMissingNode()) {
            return List.of();
        }

        List<PolicyResult> policies = new ArrayList<>();

        for (JsonNode policy : result.path(POLICIES)) {
            String policyName = policy.path(POLICY_NAME).asString(null);
            if (policyName == null) {
                continue;
            }

            JsonNode policyData = policy.path(POLICY_DATA);
            boolean allow = policyData.path(ALLOW).asBoolean();
            int violationsCount = policyData.path(VIOLATIONS).size();

            policies.add(new PolicyResult(policyName, allow, violationsCount));
        }

        return policies;
    }

    private Map<String, Integer> extractWeight(JsonNode result) {
        Map<String, Integer> weightMap = new HashMap<>();

        for (JsonNode item : result.path(SCORE).path(BREAKDOWN)) {
            String policy = item.path(POLICY).asString(null);
            if (policy != null) {
                weightMap.put(policy, item.path(WEIGHT).asInt());
            }
        }

        return weightMap;
    }

    private List<Reason> extractReasonsFromPolicyData(JsonNode policyData, String policyName, int weight) {
        List<Reason> reasons = new ArrayList<>();

        for (JsonNode ruleResult : policyData.path(RESULTS)) {
            if (ruleResult.path(ALLOW).asBoolean()) {
                continue;
            }

            String name = ruleResult.path(NAME).asString(null);
            String type = ruleResult.path(TYPE).asString(null);
            String expression = ruleResult.path(EXPRESSION).asString(null);
            Rule rule = new Rule(policyName, expression, name, type);

            for (JsonNode violation : ruleResult.path(VIOLATIONS)) {
                reasons.add(new Reason(rule, violation.asString(), weight));
            }
        }

        return reasons;
    }
}
