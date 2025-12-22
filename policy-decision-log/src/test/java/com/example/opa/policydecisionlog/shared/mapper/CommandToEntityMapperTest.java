package com.example.opa.policydecisionlog.shared.mapper;

import com.example.opa.policydecisionlog.command.app.dto.IngestDecisionLogCommand;
import com.example.opa.policydecisionlog.command.infra.model.DecisionLogRow;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import java.time.OffsetDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class CommandToRowMapperTest {

    private CommandToRowMapper mapper;
    private JsonMapper jsonMapper;

    @BeforeEach
    void setUp() {
        jsonMapper = JsonMapper.builder().build();
        mapper = new CommandToRowMapper(jsonMapper);
    }

    @Nested
    @DisplayName("toDecisionLogRow")
    class ToDecisionLogRow {

        @Test
        @DisplayName("모든 필드가 있는 Command가 주어지면 DecisionLogRow로 정상 매핑된다")
        void givenCommandWithAllFields_whenToDecisionLogRow_thenMapsCorrectly() {
            // given
            UUID decisionId = UUID.randomUUID();
            UUID opaInstanceId = UUID.randomUUID();
            UUID realmId = UUID.randomUUID();
            UUID userId = UUID.randomUUID();
            UUID userPolicyId = UUID.randomUUID();
            OffsetDateTime timestamp = OffsetDateTime.now();

            JsonNode result = jsonMapper.valueToTree(java.util.Map.of(
                    "allow", true,
                    "access_key", java.util.Map.of(
                            "realm_id", realmId.toString(),
                            "user_id", userId.toString(),
                            "user_policy_id", userPolicyId.toString()
                    ),
                    "violations", java.util.List.of(
                            java.util.Map.of("id", "v1"),
                            java.util.Map.of("id", "v2")
                    )
            ));

            JsonNode input = jsonMapper.valueToTree(java.util.Map.of(
                    "agent_data", java.util.Map.of(
                            "data", java.util.Map.of("os_type", "WINDOWS")
                    )
            ));

            JsonNode bundles = jsonMapper.valueToTree(java.util.Map.of("bundle1", "v1"));
            JsonNode raw = jsonMapper.valueToTree(java.util.Map.of("raw", "data"));

            IngestDecisionLogCommand command = IngestDecisionLogCommand.of(
                    decisionId, timestamp, "/policy/main", "user@example.com", 1L,
                    opaInstanceId, "1.0.0", bundles, input, result, raw
            );

            // when
            DecisionLogRow row = mapper.toDecisionLogRow(command);

            // then
            assertThat(row.getDecisionId()).isEqualTo(decisionId);
            assertThat(row.getTs()).isEqualTo(timestamp);
            assertThat(row.getPath()).isEqualTo("/policy/main");
            assertThat(row.isOverallAllow()).isTrue();
            assertThat(row.getRequestedBy()).isEqualTo("user@example.com");
            assertThat(row.getReqId()).isEqualTo(1L);
            assertThat(row.getOpaInstanceId()).isEqualTo(opaInstanceId);
            assertThat(row.getOpaVersion()).isEqualTo("1.0.0");
            assertThat(row.getRealmId()).isEqualTo(realmId);
            assertThat(row.getUserId()).isEqualTo(userId);
            assertThat(row.getUserPolicyId()).isEqualTo(userPolicyId);
            assertThat(row.getOsType()).isEqualTo("WINDOWS");
            assertThat(row.getViolationCount()).isEqualTo(2);
            assertThat(row.getBundles()).containsEntry("bundle1", "v1");
            assertThat(row.getRaw()).containsEntry("raw", "data");
        }

        @Test
        @DisplayName("result가 null인 Command가 주어지면 기본값으로 매핑된다")
        void givenCommandWithNullResult_whenToDecisionLogRow_thenMapsWithDefaults() {
            // given
            IngestDecisionLogCommand command = IngestDecisionLogCommand.of(
                    UUID.randomUUID(), OffsetDateTime.now(), "/policy/main",
                    null, null, null, null, null, null, null, null
            );

            // when
            DecisionLogRow row = mapper.toDecisionLogRow(command);

            // then
            assertThat(row.isOverallAllow()).isFalse();
            assertThat(row.getRealmId()).isNull();
            assertThat(row.getUserId()).isNull();
            assertThat(row.getUserPolicyId()).isNull();
            assertThat(row.getViolationCount()).isNull();
        }

        @Test
        @DisplayName("input이 null인 Command가 주어지면 osType이 null이다")
        void givenCommandWithNullInput_whenToDecisionLogRow_thenOsTypeIsNull() {
            // given
            IngestDecisionLogCommand command = IngestDecisionLogCommand.of(
                    UUID.randomUUID(), OffsetDateTime.now(), "/policy/main",
                    null, null, null, null, null, null, null, null
            );

            // when
            DecisionLogRow row = mapper.toDecisionLogRow(command);

            // then
            assertThat(row.getOsType()).isNull();
        }

        @Test
        @DisplayName("allow가 false인 result가 주어지면 overallAllow가 false이다")
        void givenResultWithAllowFalse_whenToDecisionLogRow_thenOverallAllowIsFalse() {
            // given
            JsonNode result = jsonMapper.valueToTree(java.util.Map.of("allow", false));

            IngestDecisionLogCommand command = IngestDecisionLogCommand.of(
                    UUID.randomUUID(), OffsetDateTime.now(), "/policy/main",
                    null, null, null, null, null, null, result, null
            );

            // when
            DecisionLogRow row = mapper.toDecisionLogRow(command);

            // then
            assertThat(row.isOverallAllow()).isFalse();
        }

        @Test
        @DisplayName("빈 violations 배열이 주어지면 violationCount가 0이다")
        void givenEmptyViolations_whenToDecisionLogRow_thenViolationCountIsZero() {
            // given
            JsonNode result = jsonMapper.valueToTree(java.util.Map.of(
                    "violations", java.util.List.of()
            ));

            IngestDecisionLogCommand command = IngestDecisionLogCommand.of(
                    UUID.randomUUID(), OffsetDateTime.now(), "/policy/main",
                    null, null, null, null, null, null, result, null
            );

            // when
            DecisionLogRow row = mapper.toDecisionLogRow(command);

            // then
            assertThat(row.getViolationCount()).isZero();
        }

        @Test
        @DisplayName("잘못된 UUID 형식이 주어지면 null로 매핑된다")
        void givenInvalidUuidFormat_whenToDecisionLogRow_thenReturnsNull() {
            // given
            JsonNode result = jsonMapper.valueToTree(java.util.Map.of(
                    "access_key", java.util.Map.of(
                            "realm_id", "invalid-uuid"
                    )
            ));

            IngestDecisionLogCommand command = IngestDecisionLogCommand.of(
                    UUID.randomUUID(), OffsetDateTime.now(), "/policy/main",
                    null, null, null, null, null, null, result, null
            );

            // when
            DecisionLogRow row = mapper.toDecisionLogRow(command);

            // then
            assertThat(row.getRealmId()).isNull();
        }
    }
}
