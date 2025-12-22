package com.example.opa.policydecisionlog.query.api.dto;

import com.example.opa.policydecisionlog.query.fixture.DecisionLogRowFixture;
import com.example.opa.policydecisionlog.query.infra.model.DecisionLogRow;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.OffsetDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class DecisionLogCursorResponseTest {

    @Test
    @DisplayName("결과 개수가 limit보다 많으면 nextCursor가 설정된다")
    void givenResultsExceedLimit_whenFrom_thenSetsNextCursor() {
        // given
        OffsetDateTime ts1 = OffsetDateTime.now().minusMinutes(1);
        OffsetDateTime ts2 = OffsetDateTime.now().minusMinutes(2);
        OffsetDateTime ts3 = OffsetDateTime.now().minusMinutes(3);

        DecisionLogRow row1 = DecisionLogRowFixture.createWithTimestamp(ts1);
        DecisionLogRow row2 = DecisionLogRowFixture.createWithTimestamp(ts2);
        DecisionLogRow row3 = DecisionLogRowFixture.createWithTimestamp(ts3);

        List<DecisionLogRow> rows = List.of(row1, row2, row3);
        int limit = 2;

        // when
        DecisionLogCursorResponse response = DecisionLogCursorResponse.from(rows, limit);

        // then
        assertThat(response.content()).hasSize(2);
        assertThat(response.nextCursor()).isEqualTo(ts2);
    }

    @Test
    @DisplayName("결과 개수가 limit과 같으면 nextCursor가 null이다")
    void givenResultsEqualLimit_whenFrom_thenNextCursorIsNull() {
        // given
        DecisionLogRow row1 = DecisionLogRowFixture.createDefault();
        DecisionLogRow row2 = DecisionLogRowFixture.createDefault();

        List<DecisionLogRow> rows = List.of(row1, row2);
        int limit = 2;

        // when
        DecisionLogCursorResponse response = DecisionLogCursorResponse.from(rows, limit);

        // then
        assertThat(response.content()).hasSize(2);
        assertThat(response.nextCursor()).isNull();
    }

    @Test
    @DisplayName("결과 개수가 limit보다 적으면 nextCursor가 null이다")
    void givenResultsLessThanLimit_whenFrom_thenNextCursorIsNull() {
        // given
        DecisionLogRow row = DecisionLogRowFixture.createDefault();

        List<DecisionLogRow> rows = List.of(row);
        int limit = 2;

        // when
        DecisionLogCursorResponse response = DecisionLogCursorResponse.from(rows, limit);

        // then
        assertThat(response.content()).hasSize(1);
        assertThat(response.nextCursor()).isNull();
    }

    @Test
    @DisplayName("결과가 비어있으면 빈 리스트와 null nextCursor를 반환한다")
    void givenEmptyResults_whenFrom_thenReturnsEmptyContentAndNullCursor() {
        // given
        List<DecisionLogRow> rows = List.of();
        int limit = 2;

        // when
        DecisionLogCursorResponse response = DecisionLogCursorResponse.from(rows, limit);

        // then
        assertThat(response.content()).isEmpty();
        assertThat(response.nextCursor()).isNull();
    }

}
