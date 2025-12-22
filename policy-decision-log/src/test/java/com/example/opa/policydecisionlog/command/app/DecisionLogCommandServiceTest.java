package com.example.opa.policydecisionlog.command.app;

import com.example.opa.policydecisionlog.command.app.dto.DecisionLogIngestCommand;
import com.example.opa.policydecisionlog.command.app.mapper.CommandToEntityMapper;
import com.example.opa.policydecisionlog.command.infra.DecisionLogCommandRepository;
import com.example.opa.policydecisionlog.command.infra.model.DecisionLog;
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
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.times;

@ExtendWith(MockitoExtension.class)
class DecisionLogCommandServiceTest {

    @InjectMocks
    private DecisionLogCommandService service;

    @Mock
    private DecisionLogCommandRepository repository;

    @Mock
    private CommandToEntityMapper mapper;

    @Captor
    private ArgumentCaptor<List<DecisionLog>> entitiesCaptor;

    @Nested
    @DisplayName("ingestLogs")
    class IngestLogs {

        @Test
        @DisplayName("Command 목록이 주어지면 Entity로 변환 후 저장한다")
        void givenCommands_whenIngestLogs_thenMapsAndSaves() {
            // given
            DecisionLogIngestCommand command1 = createCommand();
            DecisionLogIngestCommand command2 = createCommand();
            List<DecisionLogIngestCommand> commands = List.of(command1, command2);

            DecisionLog entity1 = createEntity();
            DecisionLog entity2 = createEntity();

            given(mapper.toEntity(command1)).willReturn(entity1);
            given(mapper.toEntity(command2)).willReturn(entity2);

            // when
            service.ingestLogs(commands);

            // then
            then(mapper).should(times(2)).toEntity(any(DecisionLogIngestCommand.class));
            then(repository).should().saveAll(entitiesCaptor.capture());

            List<DecisionLog> savedEntities = entitiesCaptor.getValue();
            assertThat(savedEntities).hasSize(2).containsExactly(entity1, entity2);
        }

        private DecisionLogIngestCommand createCommand() {
            return DecisionLogIngestCommand.of(
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

        private DecisionLog createEntity() {
            return DecisionLog.of(
                    UUID.randomUUID(),
                    OffsetDateTime.now(),
                    "/policy/main",
                    true,
                    "user@example.com",
                    1L,
                    UUID.randomUUID(),
                    "1.0.0",
                    UUID.randomUUID(),
                    UUID.randomUUID(),
                    UUID.randomUUID(),
                    "WINDOWS",
                    Map.of("bundle1", "v1"),
                    0,
                    Map.of("key", "value")
            );
        }
    }
}
