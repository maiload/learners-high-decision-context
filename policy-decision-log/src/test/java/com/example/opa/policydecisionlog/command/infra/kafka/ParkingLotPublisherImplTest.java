package com.example.opa.policydecisionlog.command.infra.kafka;

import com.example.opa.policydecisionlog.command.app.dto.DecisionLogIngestCommand;
import com.example.opa.policydecisionlog.command.infra.kafka.exception.ParkingLotPublishException;
import com.example.opa.policydecisionlog.shared.config.KafkaCustomProperties;
import com.example.opa.policydecisionlog.shared.config.KafkaCustomProperties.ProducerSettings;
import com.example.opa.policydecisionlog.shared.exception.MissingDecisionIdException;
import com.example.opa.policydecisionlog.shared.kafka.ParkingHeaders;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Answers;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import tools.jackson.databind.json.JsonMapper;

import java.nio.charset.StandardCharsets;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.*;

@ExtendWith(MockitoExtension.class)
class ParkingLotPublisherImplTest {

    private ParkingLotPublisherImpl publisher;

    @Mock
    private KafkaTemplate<String, String> kafkaTemplate;

    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private SendResult<String, String> sendResult;

    @Captor
    private ArgumentCaptor<ProducerRecord<String, String>> recordCaptor;

    private static final String PARKING_LOT_TOPIC = "decision-logs-parking";

    @BeforeEach
    void setUp() {
        JsonMapper jsonMapper = JsonMapper.builder().build();
        ProducerSettings producerSettings = new ProducerSettings(3, 30000, 10000, 5000);
        KafkaCustomProperties.ParkingRecoverySettings parkingRecovery =
                new KafkaCustomProperties.ParkingRecoverySettings(5, 60000, 2.0, 3600000);
        KafkaCustomProperties properties = new KafkaCustomProperties(
                "decision-logs", "decision-logs-dlq", PARKING_LOT_TOPIC, "decision-logs-parking-dlq",
                producerSettings, producerSettings, producerSettings, parkingRecovery,
                new KafkaCustomProperties.ConsumerBackoff(1000, 2.0, 10000, 30000)
        );
        publisher = new ParkingLotPublisherImpl(kafkaTemplate, jsonMapper, properties);
    }

    @Nested
    @DisplayName("publish")
    class Publish {

        @Test
        @DisplayName("정상 발행 시 Kafka로 전송하고 헤더 포함")
        void givenValidCommands_whenPublish_thenSendsToKafkaWithHeaders() {
            // given
            UUID decisionId = UUID.randomUUID();
            DecisionLogIngestCommand command = createCommand(decisionId);
            List<DecisionLogIngestCommand> commands = List.of(command);

            given(kafkaTemplate.send(any(ProducerRecord.class)))
                    .willReturn(CompletableFuture.completedFuture(sendResult));

            // when
            publisher.publish(commands);

            // then
            then(kafkaTemplate).should().send(recordCaptor.capture());
            ProducerRecord<String, String> captured = recordCaptor.getValue();
            assertThat(captured.topic()).isEqualTo(PARKING_LOT_TOPIC);
            assertThat(captured.key()).isEqualTo(decisionId.toString());
            assertThat(captured.headers().lastHeader(ParkingHeaders.RETRY_ATTEMPT)).isNotNull();
            assertThat(captured.headers().lastHeader(ParkingHeaders.NOT_BEFORE)).isNotNull();
            assertThat(captured.headers().lastHeader(ParkingHeaders.FIRST_FAILURE_TIME)).isNotNull();
        }

        @Test
        @DisplayName("초기 발행 시 attempt는 0")
        void givenValidCommands_whenPublish_thenAttemptIsZero() {
            // given
            UUID decisionId = UUID.randomUUID();
            DecisionLogIngestCommand command = createCommand(decisionId);
            List<DecisionLogIngestCommand> commands = List.of(command);

            given(kafkaTemplate.send(any(ProducerRecord.class)))
                    .willReturn(CompletableFuture.completedFuture(sendResult));

            // when
            publisher.publish(commands);

            // then
            then(kafkaTemplate).should().send(recordCaptor.capture());
            ProducerRecord<String, String> captured = recordCaptor.getValue();
            String attempt = new String(captured.headers().lastHeader(ParkingHeaders.RETRY_ATTEMPT).value(), StandardCharsets.UTF_8);
            assertThat(attempt).isEqualTo("0");
        }

        @Test
        @DisplayName("여러 커맨드 발행 시 각각 전송")
        void givenMultipleCommands_whenPublish_thenSendsEach() {
            // given
            UUID decisionId1 = UUID.randomUUID();
            UUID decisionId2 = UUID.randomUUID();
            List<DecisionLogIngestCommand> commands = List.of(
                    createCommand(decisionId1),
                    createCommand(decisionId2)
            );

            given(kafkaTemplate.send(any(ProducerRecord.class)))
                    .willReturn(CompletableFuture.completedFuture(sendResult));

            // when
            publisher.publish(commands);

            // then
            then(kafkaTemplate).should(times(2)).send(any(ProducerRecord.class));
        }

        @Test
        @DisplayName("decisionId가 null이면 MissingDecisionIdException 발생")
        void givenNullDecisionId_whenPublish_thenThrowsMissingDecisionIdException() {
            // given
            DecisionLogIngestCommand command = createCommand(null);
            List<DecisionLogIngestCommand> commands = List.of(command);

            // when & then
            assertThatThrownBy(() -> publisher.publish(commands))
                    .isInstanceOf(MissingDecisionIdException.class);

            then(kafkaTemplate).shouldHaveNoInteractions();
        }

        @Test
        @DisplayName("Kafka 전송 실패 시 ParkingLotPublishException 발생")
        void givenKafkaFailure_whenPublish_thenThrowsParkingLotPublishException() {
            // given
            UUID decisionId = UUID.randomUUID();
            DecisionLogIngestCommand command = createCommand(decisionId);
            List<DecisionLogIngestCommand> commands = List.of(command);

            CompletableFuture<SendResult<String, String>> failedFuture = new CompletableFuture<>();
            failedFuture.completeExceptionally(new ExecutionException(new RuntimeException("Kafka error")));
            given(kafkaTemplate.send(any(ProducerRecord.class)))
                    .willReturn(failedFuture);

            // when & then
            assertThatThrownBy(() -> publisher.publish(commands))
                    .isInstanceOf(ParkingLotPublishException.class);
        }

        @Test
        @DisplayName("Timeout 발생 시 ParkingLotPublishException 발생")
        void givenTimeout_whenPublish_thenThrowsParkingLotPublishException() {
            // given
            UUID decisionId = UUID.randomUUID();
            DecisionLogIngestCommand command = createCommand(decisionId);
            List<DecisionLogIngestCommand> commands = List.of(command);

            CompletableFuture<SendResult<String, String>> timeoutFuture = new CompletableFuture<>();
            timeoutFuture.completeExceptionally(new TimeoutException("Timeout"));
            given(kafkaTemplate.send(any(ProducerRecord.class)))
                    .willReturn(timeoutFuture);

            // when & then
            assertThatThrownBy(() -> publisher.publish(commands))
                    .isInstanceOf(ParkingLotPublishException.class);
        }

        @Test
        @DisplayName("빈 리스트면 아무 작업도 하지 않음")
        void givenEmptyList_whenPublish_thenDoesNothing() {
            // when
            publisher.publish(List.of());

            // then
            then(kafkaTemplate).shouldHaveNoInteractions();
        }
    }

    private DecisionLogIngestCommand createCommand(UUID decisionId) {
        return new DecisionLogIngestCommand(
                decisionId,
                OffsetDateTime.now(),
                "cloud_access/policy/main",
                null, null, null, null, null, null, null
        );
    }
}
