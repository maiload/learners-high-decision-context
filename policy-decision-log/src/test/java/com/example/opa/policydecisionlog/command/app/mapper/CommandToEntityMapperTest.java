package com.example.opa.policydecisionlog.command.app.mapper;

import com.example.opa.policydecisionlog.command.app.dto.DecisionLogIngestCommand;
import com.example.opa.policydecisionlog.command.infra.model.DecisionLog;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class CommandToEntityMapperTest {

    private CommandToEntityMapper mapper;
    private JsonMapper jsonMapper;

    @BeforeEach
    void setUp() {
        jsonMapper = JsonMapper.builder().build();
        mapper = new CommandToEntityMapper(jsonMapper);
    }

    @Nested
    @DisplayName("toEntity")
    class ToEntity {

        @Test
        @DisplayName("모든 필드가 있는 Command가 주어지면 DecisionLog로 정상 매핑된다")
        void givenCommandWithAllFields_whenToEntity_thenMapsCorrectly() {
            // given
            UUID decisionId = UUID.randomUUID();
            UUID opaInstanceId = UUID.randomUUID();
            UUID realmId = UUID.randomUUID();
            UUID userId = UUID.randomUUID();
            UUID userPolicyId = UUID.randomUUID();
            OffsetDateTime timestamp = OffsetDateTime.now();

            JsonNode result = jsonMapper.valueToTree(Map.of(
                    "allow", true,
                    "access_key", Map.of(
                            "realm_id", realmId.toString(),
                            "user_id", userId.toString(),
                            "user_policy_id", userPolicyId.toString()
                    ),
                    "violations", List.of(
                            Map.of("id", "v1"),
                            Map.of("id", "v2")
                    )
            ));

            JsonNode input = jsonMapper.valueToTree(Map.of(
                    "agent_data", Map.of(
                            "data", Map.of("os_type", "WINDOWS")
                    )
            ));

            JsonNode bundles = jsonMapper.valueToTree(Map.of("bundle1", "v1"));
            JsonNode raw = jsonMapper.valueToTree(Map.of("raw", "data"));

            DecisionLogIngestCommand command = DecisionLogIngestCommand.of(
                    decisionId, timestamp, "/policy/main", "user@example.com", 1L,
                    opaInstanceId, "1.0.0", bundles, input, result, raw
            );

            // when
            DecisionLog entity = mapper.toEntity(command);

            // then
            assertThat(entity.getDecisionId()).isEqualTo(decisionId);
            assertThat(entity.getTs()).isEqualTo(timestamp);
            assertThat(entity.getPath()).isEqualTo("/policy/main");
            assertThat(entity.isOverallAllow()).isTrue();
            assertThat(entity.getRequestedBy()).isEqualTo("user@example.com");
            assertThat(entity.getReqId()).isEqualTo(1L);
            assertThat(entity.getOpaInstanceId()).isEqualTo(opaInstanceId);
            assertThat(entity.getOpaVersion()).isEqualTo("1.0.0");
            assertThat(entity.getRealmId()).isEqualTo(realmId);
            assertThat(entity.getUserId()).isEqualTo(userId);
            assertThat(entity.getUserPolicyId()).isEqualTo(userPolicyId);
            assertThat(entity.getOsType()).isEqualTo("WINDOWS");
            assertThat(entity.getViolationCount()).isEqualTo(2);
            assertThat(entity.getBundles()).containsEntry("bundle1", "v1");
            assertThat(entity.getRaw()).containsEntry("raw", "data");
        }

        @Test
        @DisplayName("result가 null인 Command가 주어지면 기본값으로 매핑된다")
        void givenCommandWithNullResult_whenToEntity_thenMapsWithDefaults() {
            // given
            DecisionLogIngestCommand command = DecisionLogIngestCommand.of(
                    UUID.randomUUID(), OffsetDateTime.now(), "/policy/main",
                    null, null, null, null, null, null, null, null
            );

            // when
            DecisionLog entity = mapper.toEntity(command);

            // then
            assertThat(entity.isOverallAllow()).isFalse();
            assertThat(entity.getRealmId()).isNull();
            assertThat(entity.getUserId()).isNull();
            assertThat(entity.getUserPolicyId()).isNull();
            assertThat(entity.getViolationCount()).isNull();
        }

        @Test
        @DisplayName("input이 null인 Command가 주어지면 osType이 null이다")
        void givenCommandWithNullInput_whenToEntity_thenOsTypeIsNull() {
            // given
            DecisionLogIngestCommand command = DecisionLogIngestCommand.of(
                    UUID.randomUUID(), OffsetDateTime.now(), "/policy/main",
                    null, null, null, null, null, null, null, null
            );

            // when
            DecisionLog entity = mapper.toEntity(command);

            // then
            assertThat(entity.getOsType()).isNull();
        }

        @Test
        @DisplayName("allow가 false인 result가 주어지면 overallAllow가 false이다")
        void givenResultWithAllowFalse_whenToEntity_thenOverallAllowIsFalse() {
            // given
            JsonNode result = jsonMapper.valueToTree(Map.of("allow", false));

            DecisionLogIngestCommand command = DecisionLogIngestCommand.of(
                    UUID.randomUUID(), OffsetDateTime.now(), "/policy/main",
                    null, null, null, null, null, null, result, null
            );

            // when
            DecisionLog entity = mapper.toEntity(command);

            // then
            assertThat(entity.isOverallAllow()).isFalse();
        }

        @Test
        @DisplayName("빈 violations 배열이 주어지면 violationCount가 0이다")
        void givenEmptyViolations_whenToEntity_thenViolationCountIsZero() {
            // given
            JsonNode result = jsonMapper.valueToTree(Map.of(
                    "violations", List.of()
            ));

            DecisionLogIngestCommand command = DecisionLogIngestCommand.of(
                    UUID.randomUUID(), OffsetDateTime.now(), "/policy/main",
                    null, null, null, null, null, null, result, null
            );

            // when
            DecisionLog entity = mapper.toEntity(command);

            // then
            assertThat(entity.getViolationCount()).isZero();
        }

        @Test
        @DisplayName("잘못된 UUID 형식이 주어지면 null로 매핑된다")
        void givenInvalidUuidFormat_whenToEntity_thenReturnsNull() {
            // given
            JsonNode result = jsonMapper.valueToTree(Map.of(
                    "access_key", Map.of(
                            "realm_id", "invalid-uuid"
                    )
            ));

            DecisionLogIngestCommand command = DecisionLogIngestCommand.of(
                    UUID.randomUUID(), OffsetDateTime.now(), "/policy/main",
                    null, null, null, null, null, null, result, null
            );

            // when
            DecisionLog entity = mapper.toEntity(command);

            // then
            assertThat(entity.getRealmId()).isNull();
        }
    }
}
