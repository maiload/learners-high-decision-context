package com.example.opa.policydecisionlog.query.api.mapper;

import com.example.opa.policydecisionlog.query.api.dto.DecisionLogSearchRequest;
import com.example.opa.policydecisionlog.query.app.dto.DecisionLogSearchQuery;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.OffsetDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class RequestToQueryMapperTest {

    private RequestToQueryMapper mapper;

    @BeforeEach
    void setUp() {
        mapper = new RequestToQueryMapper();
    }

    @Nested
    @DisplayName("toQuery")
    class ToQuery {

        @Test
        @DisplayName("모든 필드가 있는 요청이 주어지면 Query로 정상 매핑된다")
        void givenRequestWithAllFields_whenToQuery_thenMapsCorrectly() {
            // given
            OffsetDateTime from = OffsetDateTime.now().minusDays(1);
            OffsetDateTime to = OffsetDateTime.now();
            OffsetDateTime cursor = OffsetDateTime.now().minusHours(1);
            UUID userId = UUID.randomUUID();
            UUID realmId = UUID.randomUUID();

            DecisionLogSearchRequest request = new DecisionLogSearchRequest(
                    from, to, true, userId, realmId, "/policy/main", 50, cursor
            );

            // when
            DecisionLogSearchQuery query = mapper.toQuery(request);

            // then
            assertThat(query.from()).isEqualTo(from);
            assertThat(query.to()).isEqualTo(to);
            assertThat(query.allow()).isTrue();
            assertThat(query.userId()).isEqualTo(userId);
            assertThat(query.realmId()).isEqualTo(realmId);
            assertThat(query.path()).isEqualTo("/policy/main");
            assertThat(query.limit()).isEqualTo(50);
            assertThat(query.cursor()).isEqualTo(cursor);
        }

        @Test
        @DisplayName("limit이 null이면 기본값 20이 적용된다")
        void givenNullLimit_whenToQuery_thenUsesDefaultLimit() {
            // given
            DecisionLogSearchRequest request = new DecisionLogSearchRequest(
                    null, null, null, null, null, null, null, null
            );

            // when
            DecisionLogSearchQuery query = mapper.toQuery(request);

            // then
            assertThat(query.limit()).isEqualTo(20);
        }

        @Test
        @DisplayName("limit이 0 이하이면 기본값 20이 적용된다")
        void givenZeroOrNegativeLimit_whenToQuery_thenUsesDefaultLimit() {
            // given
            DecisionLogSearchRequest requestZero = new DecisionLogSearchRequest(
                    null, null, null, null, null, null, 0, null
            );
            DecisionLogSearchRequest requestNegative = new DecisionLogSearchRequest(
                    null, null, null, null, null, null, -5, null
            );

            // when
            DecisionLogSearchQuery queryZero = mapper.toQuery(requestZero);
            DecisionLogSearchQuery queryNegative = mapper.toQuery(requestNegative);

            // then
            assertThat(queryZero.limit()).isEqualTo(20);
            assertThat(queryNegative.limit()).isEqualTo(20);
        }

        @Test
        @DisplayName("limit이 100을 초과하면 최대값 100이 적용된다")
        void givenLimitExceedsMax_whenToQuery_thenUsesMaxLimit() {
            // given
            DecisionLogSearchRequest request = new DecisionLogSearchRequest(
                    null, null, null, null, null, null, 150, null
            );

            // when
            DecisionLogSearchQuery query = mapper.toQuery(request);

            // then
            assertThat(query.limit()).isEqualTo(100);
        }
    }
}
