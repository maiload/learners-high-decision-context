package com.example.opa.policydecisionlog.command.infra.kafka;

import com.example.opa.policydecisionlog.command.infra.kafka.exception.DecisionLogPublishException;
import com.example.opa.policydecisionlog.shared.config.KafkaCustomProperties;
import com.example.opa.policydecisionlog.shared.config.KafkaCustomProperties.ProducerSettings;
import com.example.opa.policydecisionlog.shared.exception.MissingDecisionIdException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Answers;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import tools.jackson.databind.json.JsonMapper;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.*;

@ExtendWith(MockitoExtension.class)
class DecisionLogEventPublisherImplTest {

    private DecisionLogEventPublisherImpl publisher;

    @Mock
    private KafkaTemplate<String, String> kafkaTemplate;

    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private SendResult<String, String> sendResult;

    private static final String TOPIC = "decision-logs";

    @BeforeEach
    void setUp() {
        JsonMapper jsonMapper = JsonMapper.builder().build();
        ProducerSettings producerSettings = new ProducerSettings(3, 30000, 10000, 5000);
        KafkaCustomProperties properties = new KafkaCustomProperties(
                TOPIC, "decision-logs-dlq", "decision-logs-parking",
                producerSettings, producerSettings, producerSettings,
                new KafkaCustomProperties.ConsumerBackoff(1000, 2.0, 10000, 30000)
        );
        publisher = new DecisionLogEventPublisherImpl(kafkaTemplate, jsonMapper, properties);
    }

    @Nested
    @DisplayName("publish")
    class Publish {

        @Test
        @DisplayName("정상 발행 시 Kafka로 전송")
        void givenValidRequests_whenPublish_thenSendsToKafka() {
            // given
            UUID decisionId = UUID.randomUUID();
            Map<String, Object> request = createRequest(decisionId);
            List<Map<String, Object>> requests = List.of(request);

            given(kafkaTemplate.send(eq(TOPIC), eq(decisionId.toString()), anyString()))
                    .willReturn(CompletableFuture.completedFuture(sendResult));

            // when
            publisher.publish(requests);

            // then
            then(kafkaTemplate).should().send(eq(TOPIC), eq(decisionId.toString()), anyString());
        }

        @Test
        @DisplayName("여러 요청 발행 시 각각 전송")
        void givenMultipleRequests_whenPublish_thenSendsEach() {
            // given
            UUID decisionId1 = UUID.randomUUID();
            UUID decisionId2 = UUID.randomUUID();
            List<Map<String, Object>> requests = List.of(
                    createRequest(decisionId1),
                    createRequest(decisionId2)
            );

            given(kafkaTemplate.send(eq(TOPIC), anyString(), anyString()))
                    .willReturn(CompletableFuture.completedFuture(sendResult));

            // when
            publisher.publish(requests);

            // then
            then(kafkaTemplate).should(times(2)).send(eq(TOPIC), anyString(), anyString());
        }

        @Test
        @DisplayName("decision_id가 없으면 MissingDecisionIdException 발생")
        void givenMissingDecisionId_whenPublish_thenThrowsMissingDecisionIdException() {
            // given
            Map<String, Object> request = Map.of(
                    "path", "cloud_access/policy/main",
                    "timestamp", "2025-01-05T10:00:00Z"
            );
            List<Map<String, Object>> requests = List.of(request);

            // when & then
            assertThatThrownBy(() -> publisher.publish(requests))
                    .isInstanceOf(MissingDecisionIdException.class);

            then(kafkaTemplate).shouldHaveNoInteractions();
        }

        @Test
        @DisplayName("Kafka 전송 실패 시 DecisionLogPublishException 발생")
        void givenKafkaFailure_whenPublish_thenThrowsDecisionLogPublishException() {
            // given
            UUID decisionId = UUID.randomUUID();
            Map<String, Object> request = createRequest(decisionId);
            List<Map<String, Object>> requests = List.of(request);

            CompletableFuture<SendResult<String, String>> failedFuture = new CompletableFuture<>();
            failedFuture.completeExceptionally(new ExecutionException(new RuntimeException("Kafka error")));
            given(kafkaTemplate.send(eq(TOPIC), anyString(), anyString()))
                    .willReturn(failedFuture);

            // when & then
            assertThatThrownBy(() -> publisher.publish(requests))
                    .isInstanceOf(DecisionLogPublishException.class);
        }

        @Test
        @DisplayName("Timeout 발생 시 DecisionLogPublishException 발생")
        void givenTimeout_whenPublish_thenThrowsDecisionLogPublishException() {
            // given
            UUID decisionId = UUID.randomUUID();
            Map<String, Object> request = createRequest(decisionId);
            List<Map<String, Object>> requests = List.of(request);

            CompletableFuture<SendResult<String, String>> timeoutFuture = new CompletableFuture<>();
            timeoutFuture.completeExceptionally(new TimeoutException("Timeout"));
            given(kafkaTemplate.send(eq(TOPIC), anyString(), anyString()))
                    .willReturn(timeoutFuture);

            // when & then
            assertThatThrownBy(() -> publisher.publish(requests))
                    .isInstanceOf(DecisionLogPublishException.class);
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

    private Map<String, Object> createRequest(UUID decisionId) {
        return Map.of(
                "decision_id", decisionId.toString(),
                "path", "cloud_access/policy/main",
                "timestamp", "2025-01-05T10:00:00Z"
        );
    }
}
