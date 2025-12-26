package com.example.opa.policydecisionlog.query.api.dto;

import com.example.opa.policydecisionlog.query.app.dto.CursorPage;
import com.example.opa.policydecisionlog.query.app.dto.DecisionLogReadModel;
import com.example.opa.policydecisionlog.query.fixture.DecisionLogReadModelFixture;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.OffsetDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class DecisionLogCursorResponseTest {

    @Test
    @DisplayName("CursorPage로부터 응답을 생성한다")
    void givenCursorPage_whenFrom_thenCreatesResponse() {
        // given
        DecisionLogReadModel readModel1 = DecisionLogReadModelFixture.createDefault();
        DecisionLogReadModel readModel2 = DecisionLogReadModelFixture.createDefault();

        CursorPage<DecisionLogReadModel> page = new CursorPage<>(
                List.of(readModel1, readModel2),
                null
        );

        // when
        DecisionLogCursorResponse response = DecisionLogCursorResponse.from(page);

        // then
        assertThat(response.content()).hasSize(2);
        assertThat(response.nextCursor()).isNull();
    }

    @Test
    @DisplayName("다음 페이지가 있으면 nextCursor가 설정된다")
    void givenPageWithNextCursor_whenFrom_thenSetsNextCursor() {
        // given
        OffsetDateTime ts1 = OffsetDateTime.now().minusMinutes(1);
        OffsetDateTime ts2 = OffsetDateTime.now().minusMinutes(2);

        DecisionLogReadModel readModel1 = DecisionLogReadModelFixture.createWithTimestamp(ts1);
        DecisionLogReadModel readModel2 = DecisionLogReadModelFixture.createWithTimestamp(ts2);

        CursorPage<DecisionLogReadModel> page = new CursorPage<>(
                List.of(readModel1, readModel2),
                ts2
        );

        // when
        DecisionLogCursorResponse response = DecisionLogCursorResponse.from(page);

        // then
        assertThat(response.content()).hasSize(2);
        assertThat(response.nextCursor()).isEqualTo(ts2);
    }

    @Test
    @DisplayName("빈 CursorPage가 주어지면 빈 리스트와 null nextCursor를 반환한다")
    void givenEmptyPage_whenFrom_thenReturnsEmptyContentAndNullCursor() {
        // given
        CursorPage<DecisionLogReadModel> page = new CursorPage<>(List.of(), null);

        // when
        DecisionLogCursorResponse response = DecisionLogCursorResponse.from(page);

        // then
        assertThat(response.content()).isEmpty();
        assertThat(response.nextCursor()).isNull();
    }

    @Test
    @DisplayName("DecisionLogReadModel이 DecisionLogResponse로 변환된다")
    void givenPage_whenFrom_thenMapsToDecisionLogResponse() {
        // given
        DecisionLogReadModel readModel = DecisionLogReadModelFixture.createDefault();
        CursorPage<DecisionLogReadModel> page = new CursorPage<>(List.of(readModel), null);

        // when
        DecisionLogCursorResponse response = DecisionLogCursorResponse.from(page);

        // then
        assertThat(response.content()).hasSize(1);
        DecisionLogResponse logResponse = response.content().getFirst();
        assertThat(logResponse.decisionId()).isEqualTo(readModel.decisionId());
        assertThat(logResponse.path()).isEqualTo(readModel.path());
        assertThat(logResponse.overallAllow()).isEqualTo(readModel.overallAllow());
    }
}
