package com.example.opa.policydecisionlog.command.app;

import com.example.opa.policydecisionlog.command.app.dto.DecisionLogIngestCommand;
import com.example.opa.policydecisionlog.command.app.dto.PersistResult;
import com.example.opa.policydecisionlog.command.app.error.DataErrorException;
import com.example.opa.policydecisionlog.command.app.error.ErrorHandler;
import com.example.opa.policydecisionlog.command.app.port.DecisionLogPersistence;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.*;

@ExtendWith(MockitoExtension.class)
class PersistDecisionLogUseCaseTest {

    @InjectMocks
    private PersistDecisionLogUseCase useCase;

    @Mock
    private DecisionLogPersistence persistence;

    @Mock
    private ErrorHandler errorHandler;

    @Nested
    @DisplayName("execute")
    class Execute {

        @Test
        @DisplayName("저장 성공 시 SUCCESS 반환")
        void givenSuccessfulSave_whenExecute_thenReturnsSuccess() {
            // given
            List<DecisionLogIngestCommand> commands = List.of(createCommand());
            willDoNothing().given(persistence).saveAll(commands);

            // when
            PersistResult result = useCase.execute(commands);

            // then
            assertThat(result).isEqualTo(PersistResult.SUCCESS);
            then(persistence).should(times(1)).saveAll(commands);
            then(errorHandler).shouldHaveNoInteractions();
        }

        @Test
        @DisplayName("첫 번째 시도 실패 후 두 번째 시도 성공 시 SUCCESS 반환")
        void givenFirstFailSecondSuccess_whenExecute_thenReturnsSuccess() {
            // given
            List<DecisionLogIngestCommand> commands = List.of(createCommand());
            willThrow(new RuntimeException("DB error"))
                    .willDoNothing()
                    .given(persistence).saveAll(commands);

            // when
            PersistResult result = useCase.execute(commands);

            // then
            assertThat(result).isEqualTo(PersistResult.SUCCESS);
            then(persistence).should(times(2)).saveAll(commands);
            then(errorHandler).shouldHaveNoInteractions();
        }

        @Test
        @DisplayName("모든 재시도 실패 후 ErrorHandler가 정상 처리하면 PARKED 반환")
        void givenAllRetriesFail_whenErrorHandlerSucceeds_thenReturnsParked() {
            // given
            List<DecisionLogIngestCommand> commands = List.of(createCommand());
            RuntimeException dbError = new RuntimeException("DB error");
            willThrow(dbError).given(persistence).saveAll(commands);
            willDoNothing().given(errorHandler).handle(eq(commands), any(Throwable.class));

            // when
            PersistResult result = useCase.execute(commands);

            // then
            assertThat(result).isEqualTo(PersistResult.PARKED);
            then(persistence).should(times(2)).saveAll(commands);
            then(errorHandler).should().handle(eq(commands), any(Throwable.class));
        }

        @Test
        @DisplayName("모든 재시도 실패 후 ErrorHandler가 DataErrorException 발생 시 예외 전파")
        void givenAllRetriesFail_whenErrorHandlerThrowsDataError_thenThrows() {
            // given
            List<DecisionLogIngestCommand> commands = List.of(createCommand());
            RuntimeException dbError = new RuntimeException("DB error");
            DataErrorException dataError = new DataErrorException(0, dbError);

            willThrow(dbError).given(persistence).saveAll(commands);
            willThrow(dataError).given(errorHandler).handle(eq(commands), any(Throwable.class));

            // when & then
            assertThatThrownBy(() -> useCase.execute(commands))
                    .isInstanceOf(DataErrorException.class)
                    .hasFieldOrPropertyWithValue("failedIndex", 0);
        }

        @Test
        @DisplayName("모든 재시도 실패 후 ErrorHandler도 실패하면 FAILED 반환")
        void givenAllRetriesFail_whenErrorHandlerFails_thenReturnsFailed() {
            // given
            List<DecisionLogIngestCommand> commands = List.of(createCommand());
            RuntimeException dbError = new RuntimeException("DB error");
            RuntimeException handlerError = new RuntimeException("Handler error");

            willThrow(dbError).given(persistence).saveAll(commands);
            willThrow(handlerError).given(errorHandler).handle(eq(commands), any(Throwable.class));

            // when
            PersistResult result = useCase.execute(commands);

            // then
            assertThat(result).isEqualTo(PersistResult.FAILED);
        }

        @Test
        @DisplayName("빈 목록이 주어지면 저장 시도")
        void givenEmptyList_whenExecute_thenStillCallsPersistence() {
            // given
            List<DecisionLogIngestCommand> commands = List.of();
            willDoNothing().given(persistence).saveAll(commands);

            // when
            PersistResult result = useCase.execute(commands);

            // then
            assertThat(result).isEqualTo(PersistResult.SUCCESS);
            then(persistence).should().saveAll(commands);
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
}
