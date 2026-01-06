package com.example.opa.policydecisionlog.command.api;

import com.example.opa.policydecisionlog.command.app.PublishDecisionLogUseCase;
import com.example.opa.policydecisionlog.shared.config.GzipProperties;
import com.example.opa.policydecisionlog.shared.metrics.DecisionLogMetrics;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.BDDMockito.then;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(DecisionLogIngestController.class)
class DecisionLogIngestControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private PublishDecisionLogUseCase publishDecisionLogUseCase;

    @MockitoBean
    private GzipProperties gzipProperties;

    @MockitoBean
    private DecisionLogMetrics metrics;


    @Test
    @DisplayName("[POST] Decision Log 수집 - 정상 요청")
    void givenValidRequest_whenIngestLogs_thenReturnsNoContent() throws Exception {
        // given
        UUID decisionId = UUID.randomUUID();
        String requestBody = """
                [
                    {
                        "decision_id": "%s",
                        "timestamp": "2025-12-19T10:30:00Z",
                        "path": "cloud_access/device_posture/response",
                        "requested_by": "192.168.65.1:30825",
                        "req_id": 1,
                        "labels": {
                            "id": "%s",
                            "version": "1.0.0"
                        },
                        "bundles": {"bundle1": {"revision": "v1"}},
                        "input": {"agent_data": {"event_type": "device_posture_reported"}},
                        "result": {"allow": true}
                    }
                ]
                """.formatted(decisionId, UUID.randomUUID());

        // when & then
        mockMvc.perform(post("/logs")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isNoContent());

        then(publishDecisionLogUseCase).should().execute(anyList());
    }

    @Test
    @DisplayName("[POST] Decision Log 수집 - 빈 배열 요청")
    void givenEmptyArray_whenIngestLogs_thenProcessesSuccessfully() throws Exception {
        // given
        String requestBody = "[]";

        // when & then
        mockMvc.perform(post("/logs")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isNoContent());

        then(publishDecisionLogUseCase).should().execute(anyList());
    }

    @Test
    @DisplayName("[POST] Decision Log 수집 - 다중 로그 요청")
    @SuppressWarnings("unchecked")
    void givenMultipleLogs_whenIngestLogs_thenPassesToUseCase() throws Exception {
        // given
        UUID decisionId1 = UUID.randomUUID();
        UUID decisionId2 = UUID.randomUUID();
        String requestBody = """
                [
                    {
                        "decision_id": "%s",
                        "timestamp": "2025-12-19T10:30:00Z",
                        "path": "cloud_access/device_posture/response"
                    },
                    {
                        "decision_id": "%s",
                        "timestamp": "2025-12-19T10:31:00Z",
                        "path": "cloud_access/policy/main"
                    }
                ]
                """.formatted(decisionId1, decisionId2);

        ArgumentCaptor<List<Map<String, Object>>> captor = ArgumentCaptor.forClass(List.class);

        // when
        mockMvc.perform(post("/logs")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isNoContent());

        // then
        then(publishDecisionLogUseCase).should().execute(captor.capture());
        List<Map<String, Object>> captured = captor.getValue();
        assertThat(captured).hasSize(2);
        assertThat(captured.get(0)).containsEntry("decision_id", decisionId1.toString());
        assertThat(captured.get(1)).containsEntry("decision_id", decisionId2.toString());
    }
}
