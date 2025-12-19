package com.example.opa.policydecisionlog.shared.mapper;

import com.example.opa.policydecisionlog.command.api.dto.DecisionLogIngestRequest;
import com.example.opa.policydecisionlog.command.app.model.IngestDecisionLogCommand;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import java.time.OffsetDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class ApiToCommandMapperTest {

    private ApiToCommandMapper mapper;
    private JsonMapper jsonMapper;

    @BeforeEach
    void setUp() {
        jsonMapper = JsonMapper.builder().build();
        mapper = new ApiToCommandMapper(jsonMapper);
    }

    @Nested
    @DisplayName("toIngestDecisionLogCommand")
    class ToIngestDecisionLogCommand {

        @Test
        @DisplayName("모든 필드가 있는 요청이 주어지면 Command로 정상 매핑된다")
        void givenRequestWithAllFields_whenToCommand_thenMapsCorrectly() {
            // given
            UUID decisionId = UUID.randomUUID();
            UUID labelId = UUID.randomUUID();
            OffsetDateTime timestamp = OffsetDateTime.now();
            JsonNode bundles = jsonMapper.valueToTree(java.util.Map.of("bundle1", "v1"));
            JsonNode input = jsonMapper.valueToTree(java.util.Map.of("key", "value"));
            JsonNode result = jsonMapper.valueToTree(java.util.Map.of("allow", true));

            DecisionLogIngestRequest request = new DecisionLogIngestRequest(
                    decisionId, timestamp, "/policy/main", "user@example.com", 1L,
                    new DecisionLogIngestRequest.LabelsDto(labelId, "1.0.0"),
                    bundles, input, result
            );

            // when
            IngestDecisionLogCommand command = mapper.toIngestDecisionLogCommand(request);

            // then
            assertThat(command.decisionId()).isEqualTo(decisionId);
            assertThat(command.timestamp()).isEqualTo(timestamp);
            assertThat(command.path()).isEqualTo("/policy/main");
            assertThat(command.requestedBy()).isEqualTo("user@example.com");
            assertThat(command.reqId()).isEqualTo(1L);
            assertThat(command.opaInstanceId()).isEqualTo(labelId);
            assertThat(command.opaVersion()).isEqualTo("1.0.0");
            assertThat(command.bundles()).isEqualTo(bundles);
            assertThat(command.input()).isEqualTo(input);
            assertThat(command.result()).isEqualTo(result);
            assertThat(command.raw()).isNotNull();
        }

        @Test
        @DisplayName("labels가 null인 요청이 주어지면 opaInstanceId와 opaVersion이 null이다")
        void givenRequestWithNullLabels_whenToCommand_thenOpaFieldsAreNull() {
            // given
            DecisionLogIngestRequest request = new DecisionLogIngestRequest(
                    UUID.randomUUID(), OffsetDateTime.now(), "/policy/main",
                    null, null, null, null, null, null
            );

            // when
            IngestDecisionLogCommand command = mapper.toIngestDecisionLogCommand(request);

            // then
            assertThat(command.opaInstanceId()).isNull();
            assertThat(command.opaVersion()).isNull();
        }

        @Test
        @DisplayName("요청이 주어지면 raw 필드에 원본 데이터가 포함된다")
        void givenRequest_whenToCommand_thenRawContainsOriginalData() {
            // given
            UUID decisionId = UUID.randomUUID();
            OffsetDateTime timestamp = OffsetDateTime.now();

            DecisionLogIngestRequest request = new DecisionLogIngestRequest(
                    decisionId, timestamp, "/policy/main",
                    "user@example.com", 1L, null, null, null, null
            );

            // when
            IngestDecisionLogCommand command = mapper.toIngestDecisionLogCommand(request);

            // then
            assertThat(command.raw()).isNotNull();
            assertThat(command.raw().get("decision_id").asString()).isEqualTo(decisionId.toString());
            assertThat(command.raw().get("path").asString()).isEqualTo("/policy/main");
        }
    }
}
