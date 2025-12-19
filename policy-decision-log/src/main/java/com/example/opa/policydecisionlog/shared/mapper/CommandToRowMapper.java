package com.example.opa.policydecisionlog.shared.mapper;

import com.example.opa.policydecisionlog.command.app.model.IngestDecisionLogCommand;
import com.example.opa.policydecisionlog.command.infra.model.DecisionLogRow;
import lombok.RequiredArgsConstructor;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.JsonNode;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import tools.jackson.databind.json.JsonMapper;

import java.util.Collections;
import java.util.Map;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class CommandToRowMapper {

    private final JsonMapper jsonMapper;

    public DecisionLogRow toDecisionLogRow(IngestDecisionLogCommand command) {
        JsonNode result = command.result();
        JsonNode input = command.input();

        boolean overallAllow = extractAllow(result);
        UUID realmId = extractAccessKeyUuid(result, "realm_id");
        UUID userId = extractAccessKeyUuid(result, "user_id");
        UUID userPolicyId = extractAccessKeyUuid(result, "user_policy_id");
        String osType = extractOsType(input);
        Integer violationCount = extractViolationCount(result);

        Map<String, Object> bundles = convertToMap(command.bundles());
        Map<String, Object> raw = convertToMap(command.raw());

        return DecisionLogRow.of(
                command.decisionId(),
                command.timestamp(),
                command.path(),
                overallAllow,
                command.requestedBy(),
                command.reqId(),
                command.opaInstanceId(),
                command.opaVersion(),
                realmId,
                userId,
                userPolicyId,
                osType,
                bundles,
                violationCount,
                raw
        );
    }

    private Map<String, Object> convertToMap(JsonNode jsonNode) {
        if (jsonNode == null || jsonNode.isNull()) {
            return Collections.emptyMap();
        }
        return jsonMapper.convertValue(jsonNode, new TypeReference<>() {});
    }

    private boolean extractAllow(JsonNode result) {
        if (result == null || result.isNull()) return false;
        JsonNode allow = result.get("allow");
        if (allow != null && allow.isBoolean()) {
            return allow.asBoolean();
        }
        return false;
    }

    private UUID extractAccessKeyUuid(JsonNode result, String key) {
        if (result == null || result.isNull()) return null;
        JsonNode accessKey = result.get("access_key");
        if (accessKey == null || accessKey.isNull()) return null;
        JsonNode value = accessKey.get(key);
        if (value == null || value.isNull()) return null;
        return parseUuidOrNull(value.asString());
    }

    private String extractOsType(JsonNode input) {
        if (input == null || input.isNull()) return null;
        JsonNode agentData = input.get("agent_data");
        if (agentData == null || agentData.isNull()) return null;
        JsonNode data = agentData.get("data");
        if (data == null || data.isNull()) return null;
        JsonNode osType = data.get("os_type");
        if (osType == null || osType.isNull()) return null;
        return osType.asString();
    }

    private Integer extractViolationCount(JsonNode result) {
        if (result == null || result.isNull()) return null;
        JsonNode violations = result.get("violations");
        if (violations != null && violations.isArray()) {
            return violations.size();
        }
        return null;
    }

    private UUID parseUuidOrNull(String value) {
        if (!StringUtils.hasText(value)) return null;
        try {
            return UUID.fromString(value);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}
