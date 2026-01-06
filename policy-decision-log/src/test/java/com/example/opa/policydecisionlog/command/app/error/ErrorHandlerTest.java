package com.example.opa.policydecisionlog.command.app.error;

import com.example.opa.policydecisionlog.command.app.dto.DecisionLogIngestCommand;
import com.example.opa.policydecisionlog.command.app.port.DecisionLogPersistence;
import com.example.opa.policydecisionlog.command.app.port.ParkingLotPublisher;
import com.example.opa.policydecisionlog.shared.metrics.DecisionLogMetrics;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Answers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.sql.SQLException;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.*;

@ExtendWith(MockitoExtension.class)
class ErrorHandlerTest {

    @InjectMocks
    private ErrorHandler errorHandler;

    @Mock
    private ErrorClassifier errorClassifier;

    @Mock
    private ParkingLotPublisher parkingLotPublisher;

    @Mock
    private DecisionLogPersistence persistence;

    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private DecisionLogMetrics metrics;

    @Nested
    @DisplayName("handle - Retryable 에러")
    class HandleRetryable {

        @Test
        @DisplayName("Retryable 에러면 Parking Lot으로 발행")
        void givenRetryableError_whenHandle_thenPublishesToParkingLot() {
            // given
            List<DecisionLogIngestCommand> commands = List.of(createCommand(), createCommand());
            SQLException error = new SQLException("Connection error", "08001");

            given(errorClassifier.isRetryable(error)).willReturn(true);

            // when
            errorHandler.handle(commands, error);

            // then
            then(parkingLotPublisher).should().publish(commands);
            then(persistence).shouldHaveNoInteractions();
        }
    }

    @Nested
    @DisplayName("handle - Non-retryable 에러 (Bisect)")
    class HandleNonRetryable {

        @Test
        @DisplayName("단일 커맨드 실패 시 DataErrorException 발생")
        void givenSingleCommand_whenHandle_thenThrowsDataErrorException() {
            // given
            DecisionLogIngestCommand command = createCommand();
            List<DecisionLogIngestCommand> commands = List.of(command);
            RuntimeException error = new RuntimeException("Data error");

            given(errorClassifier.isRetryable(error)).willReturn(false);
            willThrow(error).given(persistence).saveAll(commands);

            // when & then
            assertThatThrownBy(() -> errorHandler.handle(commands, error))
                    .isInstanceOf(DataErrorException.class)
                    .hasFieldOrPropertyWithValue("failedIndex", 0)
                    .hasCause(error);
        }

        @Test
        @DisplayName("여러 커맨드 중 하나만 실패 시 Bisect로 해당 인덱스 찾음")
        void givenMultipleCommands_whenOneFails_thenBisectFindsFailingIndex() {
            // given
            DecisionLogIngestCommand cmd0 = createCommand();
            DecisionLogIngestCommand cmd1 = createCommand();
            DecisionLogIngestCommand cmd2 = createCommand();
            List<DecisionLogIngestCommand> commands = List.of(cmd0, cmd1, cmd2);
            RuntimeException error = new RuntimeException("Data error");

            given(errorClassifier.isRetryable(error)).willReturn(false);

            willAnswer(invocation -> {  // cmd2 실패
                List<?> args = invocation.getArgument(0);
                if (args.contains(cmd2)) {
                    throw error;
                }
                return null;
            }).given(persistence).saveAll(any());

            // when & then
            assertThatThrownBy(() -> errorHandler.handle(commands, error))
                    .isInstanceOf(DataErrorException.class)
                    .hasFieldOrPropertyWithValue("failedIndex", 2);
        }

        @Test
        @DisplayName("모든 커맨드가 성공하면 예외 없이 종료")
        void givenAllSucceed_whenBisect_thenNoException() {
            // given
            List<DecisionLogIngestCommand> commands = List.of(createCommand(), createCommand());
            RuntimeException error = new RuntimeException("Initial error");

            given(errorClassifier.isRetryable(error)).willReturn(false);
            willDoNothing().given(persistence).saveAll(any());

            // when
            errorHandler.handle(commands, error);

            // then - 예외 없이 정상 종료
            then(persistence).should().saveAll(commands);
        }

        @Test
        @DisplayName("빈 목록이면 아무 작업도 하지 않음")
        void givenEmptyList_whenHandle_thenDoNothing() {
            // given
            List<DecisionLogIngestCommand> commands = List.of();
            RuntimeException error = new RuntimeException("Error");

            given(errorClassifier.isRetryable(error)).willReturn(false);

            // when
            errorHandler.handle(commands, error);

            // then
            then(persistence).shouldHaveNoInteractions();
            then(parkingLotPublisher).shouldHaveNoInteractions();
        }
    }

    private DecisionLogIngestCommand createCommand() {
        return new DecisionLogIngestCommand(
                UUID.randomUUID(),
                OffsetDateTime.now(),
                "cloud_access/policy/main",
                null, null, null, null, null, null, null
        );
    }
}
