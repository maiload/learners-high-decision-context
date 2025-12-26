package com.example.opa.policydecisionlog.command.infra.mapper;

import com.example.opa.policydecisionlog.command.app.dto.DecisionLogIngestCommand;
import com.example.opa.policydecisionlog.command.infra.model.DecisionLogEntity;
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
        @DisplayName("모든 필드가 있는 Command가 주어지면 DecisionLogEntity로 정상 매핑된다")
        void givenCommandWithAllFields_whenToEntity_thenMapsCorrectly() {
            // given
            UUID decisionId = UUID.randomUUID();
            UUID opaInstanceId = UUID.randomUUID();
            OffsetDateTime timestamp = OffsetDateTime.now();

            JsonNode result = jsonMapper.valueToTree(Map.of("allow", true));
            JsonNode bundles = jsonMapper.valueToTree(Map.of("bundle1", "v1"));
            JsonNode raw = jsonMapper.valueToTree(Map.of("raw", "data"));

            DecisionLogIngestCommand command = new DecisionLogIngestCommand(
                    decisionId, timestamp, "/cloud_access/policy/main", "user@example.com", 1L,
                    opaInstanceId, "1.0.0", bundles, null, result, raw
            );

            // when
            DecisionLogEntity entity = mapper.toEntity(command);

            // then
            assertThat(entity.getDecisionId()).isEqualTo(decisionId);
            assertThat(entity.getTs()).isEqualTo(timestamp);
            assertThat(entity.getPath()).isEqualTo("/cloud_access/policy/main");
            assertThat(entity.isOverallAllow()).isTrue();
            assertThat(entity.getRequestedBy()).isEqualTo("user@example.com");
            assertThat(entity.getReqId()).isEqualTo(1L);
            assertThat(entity.getOpaInstanceId()).isEqualTo(opaInstanceId);
            assertThat(entity.getOpaVersion()).isEqualTo("1.0.0");
            assertThat(entity.getService()).isEqualTo("cloud_access");
            assertThat(entity.getBundles()).containsEntry("bundle1", "v1");
            assertThat(entity.getRaw()).containsEntry("raw", "data");
        }

        @Test
        @DisplayName("result가 null인 Command가 주어지면 overallAllow가 false이다")
        void givenCommandWithNullResult_whenToEntity_thenOverallAllowIsFalse() {
            // given
            DecisionLogIngestCommand command = new DecisionLogIngestCommand(
                    UUID.randomUUID(), OffsetDateTime.now(), "/policy/main",
                    null, null, null, null, null, null, null, null
            );

            // when
            DecisionLogEntity entity = mapper.toEntity(command);

            // then
            assertThat(entity.isOverallAllow()).isFalse();
        }

        @Test
        @DisplayName("allow가 false인 result가 주어지면 overallAllow가 false이다")
        void givenResultWithAllowFalse_whenToEntity_thenOverallAllowIsFalse() {
            // given
            JsonNode result = jsonMapper.valueToTree(Map.of("allow", false));

            DecisionLogIngestCommand command = new DecisionLogIngestCommand(
                    UUID.randomUUID(), OffsetDateTime.now(), "/policy/main",
                    null, null, null, null, null, null, result, null
            );

            // when
            DecisionLogEntity entity = mapper.toEntity(command);

            // then
            assertThat(entity.isOverallAllow()).isFalse();
        }

        @Test
        @DisplayName("path에서 service를 추출한다")
        void givenPathWithService_whenToEntity_thenExtractsService() {
            // given
            DecisionLogIngestCommand command = new DecisionLogIngestCommand(
                    UUID.randomUUID(), OffsetDateTime.now(), "/cloud_access/device_posture/response",
                    null, null, null, null, null, null, null, null
            );

            // when
            DecisionLogEntity entity = mapper.toEntity(command);

            // then
            assertThat(entity.getService()).isEqualTo("cloud_access");
        }

        @Test
        @DisplayName("bundles가 null이면 빈 Map이 저장된다")
        void givenNullBundles_whenToEntity_thenEmptyMap() {
            // given
            DecisionLogIngestCommand command = new DecisionLogIngestCommand(
                    UUID.randomUUID(), OffsetDateTime.now(), "/policy/main",
                    null, null, null, null, null, null, null, null
            );

            // when
            DecisionLogEntity entity = mapper.toEntity(command);

            // then
            assertThat(entity.getBundles()).isEmpty();
        }
    }
}
