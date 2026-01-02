package com.example.opa.policydecisionlog.command.infra.db.mapper;

import com.example.opa.policydecisionlog.command.app.dto.DecisionLogIngestCommand;
import com.example.opa.policydecisionlog.command.infra.db.model.DecisionLogEntity;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import java.util.Map;

@Component
@RequiredArgsConstructor
public class CommandToEntityMapper {

    private final JsonMapper jsonMapper;

    public DecisionLogEntity toEntity(DecisionLogIngestCommand command) {
        JsonNode result = command.result();

        boolean overallAllow = extractAllow(result);
        String service = extractService(command.path());

        Map<String, Object> bundles = convertToMap(command.bundles());
        Map<String, Object> raw = convertToMap(command.raw());

        return DecisionLogEntity.of(
                command.decisionId(),
                command.timestamp(),
                command.path(),
                overallAllow,
                command.requestedBy(),
                command.reqId(),
                command.opaInstanceId(),
                command.opaVersion(),
                service,
                bundles,
                raw
        );
    }

    private Map<String, Object> convertToMap(JsonNode jsonNode) {
        if (jsonNode == null || jsonNode.isNull()) {
            return Map.of();
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

    private String extractService(String path) {
        if (!StringUtils.hasText(path)) {
            return null;
        }
        String[] segments = path.split("/");
        for (String segment : segments) {
            if (!segment.isBlank()) {
                return segment;
            }
        }
        return null;
    }
}
