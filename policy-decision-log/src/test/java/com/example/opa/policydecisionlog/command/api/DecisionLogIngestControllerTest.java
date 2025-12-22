package com.example.opa.policydecisionlog.command.api;

import com.example.opa.policydecisionlog.command.api.mapper.RequestToCommandMapper;
import com.example.opa.policydecisionlog.command.app.DecisionLogCommandService;
import com.example.opa.policydecisionlog.command.app.dto.DecisionLogIngestCommand;
import com.example.opa.policydecisionlog.shared.config.GzipProperties;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.OffsetDateTime;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.times;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(DecisionLogIngestController.class)
class DecisionLogIngestControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private DecisionLogCommandService commandService;

    @MockitoBean
    private RequestToCommandMapper requestToCommandMapper;

    @MockitoBean
    private GzipProperties gzipProperties;

    @Test
    @DisplayName("[POST] Decision Log 수집 - 정상 호출")
    void givenValidRequest_whenIngestLogs_thenReturnsNoContent() throws Exception {
        // given
        UUID decisionId = UUID.randomUUID();
        String requestBody = """
                [
                    {
                        "decision_id": "%s",
                        "timestamp": "2025-12-19T10:30:00Z",
                        "path": "/policy/main",
                        "requested_by": "user@example.com",
                        "req_id": 1,
                        "labels": {
                            "id": "%s",
                            "version": "1.0.0"
                        },
                        "bundles": {"bundle1": "v1"},
                        "input": {"key": "value"},
                        "result": {"allow": true}
                    }
                ]
                """.formatted(decisionId, UUID.randomUUID());

        given(requestToCommandMapper.toCommand(any()))
                .willReturn(DecisionLogIngestCommand.of(
                        decisionId, OffsetDateTime.now(), "/policy/main",
                        "user@example.com", 1L, null, null, null, null, null, null
                ));

        // when
        mockMvc.perform(post("/logs")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isNoContent());

        // then
        then(commandService).should().ingestLogs(any());
    }

    @Test
    @DisplayName("[POST] Decision Log 수집 - 빈 배열 요청")
    void givenEmptyArray_whenIngestLogs_thenProcessesSuccessfully() throws Exception {
        // given
        String requestBody = "[]";

        // when
        mockMvc.perform(post("/logs")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isNoContent());

        // then
        then(commandService).should().ingestLogs(any());
    }

    @Test
    @DisplayName("[POST] Decision Log 수집 - 다중 로그 요청")
    void givenMultipleLogs_whenIngestLogs_thenProcessesAll() throws Exception {
        // given
        UUID decisionId1 = UUID.randomUUID();
        UUID decisionId2 = UUID.randomUUID();
        String requestBody = """
                [
                    {
                        "decision_id": "%s",
                        "timestamp": "2025-12-19T10:30:00Z",
                        "path": "/policy/main"
                    },
                    {
                        "decision_id": "%s",
                        "timestamp": "2025-12-19T10:31:00Z",
                        "path": "/policy/sub"
                    }
                ]
                """.formatted(decisionId1, decisionId2);

        given(requestToCommandMapper.toCommand(any()))
                .willReturn(DecisionLogIngestCommand.of(
                        UUID.randomUUID(), OffsetDateTime.now(), "/policy/main",
                        null, null, null, null, null, null, null, null
                ));

        // when
        mockMvc.perform(post("/logs")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isNoContent());

        // then
        then(requestToCommandMapper).should(times(2)).toCommand(any());
    }
}
