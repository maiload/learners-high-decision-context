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

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
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
        @DisplayName("단일 커맨드 실패 시 DLQ로 전송")
        void givenSingleCommand_whenHandle_thenSendsToDlq() {
            // given
            DecisionLogIngestCommand command = createCommand();
            List<DecisionLogIngestCommand> commands = List.of(command);
            RuntimeException error = new RuntimeException("Data error");

            given(errorClassifier.isRetryable(error)).willReturn(false);
            willThrow(error).given(persistence).saveAll(commands);

            // when
            errorHandler.handle(commands, error);

            // then
            then(parkingLotPublisher).should().toDlq(command);
            then(metrics).should().recordDlqSent(1);
        }

        @Test
        @DisplayName("여러 커맨드 중 하나만 실패 시 Bisect로 찾아서 DLQ로 전송")
        void givenMultipleCommands_whenOneFails_thenBisectFindsAndSendsToDlq() {
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

            // when
            errorHandler.handle(commands, error);

            // then
            then(parkingLotPublisher).should().toDlq(cmd2);
            then(parkingLotPublisher).should(never()).toDlq(cmd0);
            then(parkingLotPublisher).should(never()).toDlq(cmd1);
            then(metrics).should().recordDlqSent(1);
        }

        @Test
        @DisplayName("모든 커맨드가 성공하면 DLQ 전송 없음")
        void givenAllSucceed_whenBisect_thenNoDlqSent() {
            // given
            List<DecisionLogIngestCommand> commands = List.of(createCommand(), createCommand());
            RuntimeException error = new RuntimeException("Initial error");

            given(errorClassifier.isRetryable(error)).willReturn(false);
            willDoNothing().given(persistence).saveAll(any());

            // when
            errorHandler.handle(commands, error);

            // then
            then(persistence).should().saveAll(commands);
            then(parkingLotPublisher).should(never()).toDlq(any());
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

    @Nested
    @DisplayName("handleRecovery")
    class HandleRecovery {

        @Test
        @DisplayName("Retryable 에러면 retry로 재발행")
        void givenRetryableError_whenHandleRecovery_thenRetries() {
            // given
            DecisionLogIngestCommand command = createCommand();
            SQLException error = new SQLException("Connection error", "08001");
            int attempt = 1;

            given(errorClassifier.isRetryable(error)).willReturn(true);

            // when
            errorHandler.handleRecovery(command, attempt, error);

            // then
            then(parkingLotPublisher).should().retry(command, attempt + 1);
            then(metrics).should().recordParkingSent(1);
            then(parkingLotPublisher).should(never()).toDlq(any());
        }

        @Test
        @DisplayName("Non-retryable 에러면 DLQ로 전송")
        void givenNonRetryableError_whenHandleRecovery_thenSendsToDlq() {
            // given
            DecisionLogIngestCommand command = createCommand();
            RuntimeException error = new RuntimeException("Data error");
            int attempt = 1;

            given(errorClassifier.isRetryable(error)).willReturn(false);

            // when
            errorHandler.handleRecovery(command, attempt, error);

            // then
            then(parkingLotPublisher).should().toDlq(command);
            then(metrics).should().recordDlqSent(1);
            then(parkingLotPublisher).should(never()).retry(any(), anyInt());
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
