package com.example.opa.policydecisionlog.query.app;

import com.example.opa.policydecisionlog.query.app.dto.DecisionLogSearchQuery;
import com.example.opa.policydecisionlog.query.fixture.DecisionLogRowFixture;
import com.example.opa.policydecisionlog.query.infra.DecisionLogQueryRepository;
import com.example.opa.policydecisionlog.query.infra.model.DecisionLogRow;
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

    @Nested
    @DisplayName("getByDecisionId")
    class GetByDecisionId {

        @Test
        @DisplayName("존재하는 decisionId가 주어지면 DecisionLogRow를 반환한다")
        void givenExistingDecisionId_whenGetByDecisionId_thenReturnsDecisionLogRow() {
            // given
            UUID decisionId = UUID.randomUUID();
            DecisionLogRow row = DecisionLogRowFixture.createDefault();

            given(repository.findByDecisionId(decisionId)).willReturn(Optional.of(row));

            // when
            DecisionLogRow result = service.getByDecisionId(decisionId);

            // then
            assertThat(result).isEqualTo(row);
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
        @DisplayName("검색 조건이 주어지면 조건에 맞는 결과를 반환한다")
        void givenSearchQuery_whenSearch_thenReturnsMatchingResults() {
            // given
            DecisionLogSearchQuery query = DecisionLogSearchQuery.of(
                    OffsetDateTime.now().minusDays(1),
                    OffsetDateTime.now(),
                    true,
                    UUID.randomUUID(),
                    UUID.randomUUID(),
                    "/policy/main",
                    20,
                    null
            );

            DecisionLogRow row1 = DecisionLogRowFixture.createDefault();
            DecisionLogRow row2 = DecisionLogRowFixture.createDefault();
            List<DecisionLogRow> expectedRows = List.of(row1, row2);

            given(repository.search(query)).willReturn(expectedRows);

            // when
            List<DecisionLogRow> result = service.search(query);

            // then
            assertThat(result).hasSize(2).containsExactly(row1, row2);
            then(repository).should().search(query);
        }

        @Test
        @DisplayName("검색 결과가 없으면 빈 리스트를 반환한다")
        void givenNoMatchingResults_whenSearch_thenReturnsEmptyList() {
            // given
            DecisionLogSearchQuery query = DecisionLogSearchQuery.of(
                    null, null, null, null, null, null, 20, null
            );

            given(repository.search(query)).willReturn(List.of());

            // when
            List<DecisionLogRow> result = service.search(query);

            // then
            assertThat(result).isEmpty();
            then(repository).should().search(query);
        }

        @Test
        @DisplayName("cursor가 주어지면 해당 조건으로 검색한다")
        void givenCursor_whenSearch_thenSearchesWithCursor() {
            // given
            OffsetDateTime cursor = OffsetDateTime.now().minusHours(1);
            DecisionLogSearchQuery query = DecisionLogSearchQuery.of(
                    null, null, null, null, null, null, 20, cursor
            );

            DecisionLogRow row = DecisionLogRowFixture.createDefault();
            given(repository.search(query)).willReturn(List.of(row));

            // when
            List<DecisionLogRow> result = service.search(query);

            // then
            assertThat(result).hasSize(1);
            then(repository).should().search(query);
        }
    }
}
