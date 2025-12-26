package com.example.opa.policydecisionlog.query.app;

import com.example.opa.policydecisionlog.query.app.dto.CursorPage;
import com.example.opa.policydecisionlog.query.app.dto.DecisionLogReadModel;
import com.example.opa.policydecisionlog.query.app.dto.DecisionLogSearchQuery;
import com.example.opa.policydecisionlog.query.fixture.DecisionLogReadModelFixture;
import com.example.opa.policydecisionlog.shared.exception.DecisionNotFoundException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

@ExtendWith(MockitoExtension.class)
class DecisionLogQueryServiceTest {

    @InjectMocks
    private DecisionLogQueryService service;

    @Mock
    private DecisionLogQueryRepository repository;

    @Mock
    private DecisionContextAssembler contextAssembler;

    @Nested
    @DisplayName("getByDecisionId")
    class GetByDecisionId {

        @Test
        @DisplayName("존재하는 decisionId가 주어지면 DecisionLogReadModel을 반환한다")
        void givenExistingDecisionId_whenGetByDecisionId_thenReturnsDecisionLogReadModel() {
            // given
            UUID decisionId = UUID.randomUUID();
            DecisionLogReadModel readModel = DecisionLogReadModelFixture.createWithDecisionId(decisionId);

            given(repository.findByDecisionId(decisionId)).willReturn(Optional.of(readModel));

            // when
            DecisionLogReadModel result = service.getByDecisionId(decisionId);

            // then
            assertThat(result).isEqualTo(readModel);
            then(repository).should().findByDecisionId(decisionId);
        }

        @Test
        @DisplayName("존재하지 않는 decisionId가 주어지면 DecisionNotFoundException을 던진다")
        void givenNonExistingDecisionId_whenGetByDecisionId_thenThrowsDecisionNotFoundException() {
            // given
            UUID decisionId = UUID.randomUUID();

            given(repository.findByDecisionId(decisionId)).willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> service.getByDecisionId(decisionId))
                    .isInstanceOf(DecisionNotFoundException.class)
                    .hasMessageContaining(decisionId.toString());

            then(repository).should().findByDecisionId(decisionId);
        }
    }

    @Nested
    @DisplayName("search")
    class Search {

        @Test
        @DisplayName("검색 조건이 주어지면 CursorPage로 결과를 반환한다")
        void givenSearchQuery_whenSearch_thenReturnsCursorPage() {
            // given
            DecisionLogSearchQuery query = new DecisionLogSearchQuery(
                    OffsetDateTime.now().minusDays(1),
                    OffsetDateTime.now(),
                    true,
                    "cloud_access",
                    "/policy/main",
                    20,
                    null
            );

            DecisionLogReadModel readModel1 = DecisionLogReadModelFixture.createDefault();
            DecisionLogReadModel readModel2 = DecisionLogReadModelFixture.createDefault();

            given(repository.search(query)).willReturn(List.of(readModel1, readModel2));

            // when
            CursorPage<DecisionLogReadModel> result = service.search(query);

            // then
            assertThat(result.content()).hasSize(2).containsExactly(readModel1, readModel2);
            assertThat(result.nextCursor()).isNull();
            then(repository).should().search(query);
        }

        @Test
        @DisplayName("검색 결과가 없으면 빈 CursorPage를 반환한다")
        void givenNoMatchingResults_whenSearch_thenReturnsEmptyCursorPage() {
            // given
            DecisionLogSearchQuery query = new DecisionLogSearchQuery(
                    null, null, null, null, null, 20, null
            );

            given(repository.search(query)).willReturn(List.of());

            // when
            CursorPage<DecisionLogReadModel> result = service.search(query);

            // then
            assertThat(result.content()).isEmpty();
            assertThat(result.nextCursor()).isNull();
            then(repository).should().search(query);
        }

        @Test
        @DisplayName("결과가 limit보다 많으면 nextCursor가 설정된다")
        void givenMoreResultsThanLimit_whenSearch_thenSetsNextCursor() {
            // given
            int limit = 2;
            DecisionLogSearchQuery query = new DecisionLogSearchQuery(
                    null, null, null, null, null, limit, null
            );

            OffsetDateTime ts1 = OffsetDateTime.now().minusHours(1);
            OffsetDateTime ts2 = OffsetDateTime.now().minusHours(2);
            OffsetDateTime ts3 = OffsetDateTime.now().minusHours(3);

            DecisionLogReadModel readModel1 = DecisionLogReadModelFixture.createWithTimestamp(ts1);
            DecisionLogReadModel readModel2 = DecisionLogReadModelFixture.createWithTimestamp(ts2);
            DecisionLogReadModel readModel3 = DecisionLogReadModelFixture.createWithTimestamp(ts3);

            // limit + 1 개 반환하여 다음 페이지 존재 표시
            given(repository.search(query)).willReturn(List.of(readModel1, readModel2, readModel3));

            // when
            CursorPage<DecisionLogReadModel> result = service.search(query);

            // then
            assertThat(result.content()).hasSize(2);
            assertThat(result.nextCursor()).isEqualTo(ts2);
        }
    }
}
