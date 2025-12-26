package com.example.opa.policydecisionlog.command.app;

import com.example.opa.policydecisionlog.command.app.dto.DecisionLogIngestCommand;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.then;

@ExtendWith(MockitoExtension.class)
class DecisionLogCommandServiceTest {

    @InjectMocks
    private DecisionLogCommandService service;

    @Mock
    private DecisionLogCommandRepository repository;

    @Captor
    private ArgumentCaptor<List<DecisionLogIngestCommand>> commandsCaptor;

    @Nested
    @DisplayName("ingestLogs")
    class IngestLogs {

        @Test
        @DisplayName("Command 목록이 주어지면 Repository에 저장을 위임한다")
        void givenCommands_whenIngestLogs_thenDelegatesToRepository() {
            // given
            DecisionLogIngestCommand command1 = createCommand();
            DecisionLogIngestCommand command2 = createCommand();
            List<DecisionLogIngestCommand> commands = List.of(command1, command2);

            // when
            service.ingestLogs(commands);

            // then
            then(repository).should().saveAll(commandsCaptor.capture());

            List<DecisionLogIngestCommand> savedCommands = commandsCaptor.getValue();
            assertThat(savedCommands).hasSize(2).containsExactly(command1, command2);
        }

        private DecisionLogIngestCommand createCommand() {
            return new DecisionLogIngestCommand(
                    UUID.randomUUID(),
                    OffsetDateTime.now(),
                    "/policy/main",
                    "user@example.com",
                    1L,
                    UUID.randomUUID(),
                    "1.0.0",
                    null,
                    null,
                    null,
                    null
            );
        }
    }
}
