package com.example.opa.policydecisionlog.query.api;

import com.example.opa.policydecisionlog.query.api.mapper.RequestToQueryMapper;
import com.example.opa.policydecisionlog.query.app.DecisionLogQueryService;
import com.example.opa.policydecisionlog.query.app.dto.DecisionLogSearchQuery;
import com.example.opa.policydecisionlog.query.fixture.DecisionLogRowFixture;
import com.example.opa.policydecisionlog.query.infra.model.DecisionLogRow;
import com.example.opa.policydecisionlog.shared.config.GzipProperties;
import com.example.opa.policydecisionlog.shared.exception.DecisionNotFoundException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(DecisionLogQueryController.class)
class DecisionLogQueryControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private DecisionLogQueryService queryService;

    @MockitoBean
    private RequestToQueryMapper mapper;

    @MockitoBean
    private GzipProperties gzipProperties;

    @Nested
    @DisplayName("GET /decisions/{decisionId}")
    class GetDecision {

        @Test
        @DisplayName("[GET] Decision Log 단건 조회 - 정상 호출")
        void givenExistingDecisionId_whenGetDecision_thenReturnsOkWithDecisionLog() throws Exception {
            // given
            UUID decisionId = UUID.randomUUID();
            DecisionLogRow row = DecisionLogRowFixture.createDefault();

            given(queryService.getByDecisionId(decisionId)).willReturn(row);

            // when & then
            mockMvc.perform(get("/decisions/{decisionId}", decisionId))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.decisionId").value(row.decisionId().toString()))
                    .andExpect(jsonPath("$.path").value(row.path()))
                    .andExpect(jsonPath("$.allow").value(row.overallAllow()));

            then(queryService).should().getByDecisionId(decisionId);
        }

        @Test
        @DisplayName("[GET] Decision Log 단건 조회 - 존재하지 않는 ID")
        void givenNonExistingDecisionId_whenGetDecision_thenReturnsNotFound() throws Exception {
            // given
            UUID decisionId = UUID.randomUUID();

            given(queryService.getByDecisionId(decisionId))
                    .willThrow(new DecisionNotFoundException(decisionId));

            // when & then
            mockMvc.perform(get("/decisions/{decisionId}", decisionId))
                    .andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("[GET] Decision Log 단건 조회 - 잘못된 UUID 형식")
        void givenInvalidUuidFormat_whenGetDecision_thenReturnsBadRequest() throws Exception {
            // when & then
            mockMvc.perform(get("/decisions/{decisionId}", "invalid-uuid"))
                    .andExpect(status().isBadRequest());
        }
    }

    @Nested
    @DisplayName("GET /decisions")
    class SearchDecisions {

        @Test
        @DisplayName("[GET] Decision Log 목록 조회 - 정상 호출")
        void givenSearchParams_whenSearchDecisions_thenReturnsOkWithResults() throws Exception {
            // given
            DecisionLogRow row = DecisionLogRowFixture.createDefault();
            DecisionLogSearchQuery query = DecisionLogSearchQuery.of(
                    null, null, null, null, null, null, 20, null
            );

            given(mapper.toQuery(any())).willReturn(query);
            given(queryService.search(query)).willReturn(List.of(row));

            // when & then
            mockMvc.perform(get("/decisions"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content").isArray())
                    .andExpect(jsonPath("$.content.length()").value(1))
                    .andExpect(jsonPath("$.nextCursor").doesNotExist());

            then(mapper).should().toQuery(any());
            then(queryService).should().search(query);
        }

        @Test
        @DisplayName("[GET] Decision Log 목록 조회 - 빈 결과")
        void givenNoResults_whenSearchDecisions_thenReturnsEmptyArray() throws Exception {
            // given
            DecisionLogSearchQuery query = DecisionLogSearchQuery.of(
                    null, null, null, null, null, null, 20, null
            );

            given(mapper.toQuery(any())).willReturn(query);
            given(queryService.search(query)).willReturn(List.of());

            // when & then
            mockMvc.perform(get("/decisions"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content").isArray())
                    .andExpect(jsonPath("$.content.length()").value(0))
                    .andExpect(jsonPath("$.nextCursor").doesNotExist());
        }

        @Test
        @DisplayName("[GET] Decision Log 목록 조회 - 다음 페이지 존재")
        void givenMoreResultsExist_whenSearchDecisions_thenReturnsNextCursor() throws Exception {
            // given
            OffsetDateTime ts1 = OffsetDateTime.now().minusMinutes(1);
            OffsetDateTime ts2 = OffsetDateTime.now().minusMinutes(2);
            OffsetDateTime ts3 = OffsetDateTime.now().minusMinutes(3);

            DecisionLogRow row1 = DecisionLogRowFixture.createWithTimestamp(ts1);
            DecisionLogRow row2 = DecisionLogRowFixture.createWithTimestamp(ts2);
            DecisionLogRow row3 = DecisionLogRowFixture.createWithTimestamp(ts3);

            DecisionLogSearchQuery query = DecisionLogSearchQuery.of(
                    null, null, null, null, null, null, 2, null
            );

            given(mapper.toQuery(any())).willReturn(query);
            given(queryService.search(query)).willReturn(List.of(row1, row2, row3));

            // when & then
            mockMvc.perform(get("/decisions").param("limit", "2"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content.length()").value(2))
                    .andExpect(jsonPath("$.nextCursor").exists());
        }

        @Test
        @DisplayName("[GET] Decision Log 목록 조회 - 필터 파라미터 적용")
        void givenFilterParams_whenSearchDecisions_thenFiltersResults() throws Exception {
            // given
            UUID userId = UUID.randomUUID();
            UUID realmId = UUID.randomUUID();
            DecisionLogSearchQuery query = DecisionLogSearchQuery.of(
                    null, null, true, userId, realmId, "/policy/main", 20, null
            );

            given(mapper.toQuery(any())).willReturn(query);
            given(queryService.search(query)).willReturn(List.of());

            // when & then
            mockMvc.perform(get("/decisions")
                            .param("allow", "true")
                            .param("userId", userId.toString())
                            .param("realmId", realmId.toString())
                            .param("path", "/policy/main"))
                    .andExpect(status().isOk());

            then(mapper).should().toQuery(any());
            then(queryService).should().search(query);
        }
    }
}
