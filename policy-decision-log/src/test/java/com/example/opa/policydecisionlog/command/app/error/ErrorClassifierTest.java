package com.example.opa.policydecisionlog.command.app.error;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.sql.SQLException;

import static org.assertj.core.api.Assertions.assertThat;

class ErrorClassifierTest {

    private ErrorClassifier classifier;

    @BeforeEach
    void setUp() {
        classifier = new ErrorClassifier();
    }

    @Nested
    @DisplayName("isRetryable")
    class IsRetryable {

        @Test
        @DisplayName("SQLException이 아니면 false 반환")
        void givenNonSqlException_whenIsRetryable_thenReturnsFalse() {
            // given
            RuntimeException ex = new RuntimeException("Some error");

            // when
            boolean result = classifier.isRetryable(ex);

            // then
            assertThat(result).isFalse();
        }

        @ParameterizedTest
        @ValueSource(strings = {"08000", "08001", "08003", "08006", "40P01", "55P03", "57014", "53300", "57P01"})
        @DisplayName("Retryable SQL State면 true 반환")
        void givenRetryableSqlState_whenIsRetryable_thenReturnsTrue(String sqlState) {
            // given
            SQLException ex = new SQLException("Error", sqlState);

            // when
            boolean result = classifier.isRetryable(ex);

            // then
            assertThat(result).isTrue();
        }

        @ParameterizedTest
        @ValueSource(strings = {"08999", "08ABC"})
        @DisplayName("08로 시작하는 SQL State면 true 반환")
        void givenSqlStateStartsWith08_whenIsRetryable_thenReturnsTrue(String sqlState) {
            // given
            SQLException ex = new SQLException("Connection error", sqlState);

            // when
            boolean result = classifier.isRetryable(ex);

            // then
            assertThat(result).isTrue();
        }

        @ParameterizedTest
        @ValueSource(strings = {"23505", "22001", "42P01", "23503"})
        @DisplayName("Non-retryable SQL State면 false 반환")
        void givenNonRetryableSqlState_whenIsRetryable_thenReturnsFalse(String sqlState) {
            // given
            SQLException ex = new SQLException("Data error", sqlState);

            // when
            boolean result = classifier.isRetryable(ex);

            // then
            assertThat(result).isFalse();
        }

        @Test
        @DisplayName("래핑된 SQLException도 true 반환")
        void givenWrappedSqlException_whenIsRetryable_thenReturnsTrue() {
            // given
            SQLException sqlEx = new SQLException("Connection refused", "08001");
            RuntimeException wrapper = new RuntimeException("Wrapper", sqlEx);

            // when
            boolean result = classifier.isRetryable(wrapper);

            // then
            assertThat(result).isTrue();
        }

        @Test
        @DisplayName("SQL State가 null이면 false 반환")
        void givenNullSqlState_whenIsRetryable_thenReturnsFalse() {
            // given
            SQLException ex = new SQLException("Error", (String) null);

            // when
            boolean result = classifier.isRetryable(ex);

            // then
            assertThat(result).isFalse();
        }

        @Test
        @DisplayName("중첩된 SQLException도 true 반환")
        void givenNestedSqlException_whenIsRetryable_thenReturnsTrue() {
            // given
            SQLException sqlEx = new SQLException("Deadlock", "40P01");
            RuntimeException level1 = new RuntimeException("Level 1", sqlEx);
            RuntimeException level2 = new RuntimeException("Level 2", level1);

            // when
            boolean result = classifier.isRetryable(level2);

            // then
            assertThat(result).isTrue();
        }
    }
}
