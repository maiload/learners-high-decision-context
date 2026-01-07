package com.example.opa.policydecisionlog.command.infra.kafka;

import com.example.opa.policydecisionlog.command.app.PersistDecisionLogUseCase;
import com.example.opa.policydecisionlog.command.app.dto.DecisionLogIngestCommand;
import com.example.opa.policydecisionlog.command.app.dto.PersistResult;
import com.example.opa.policydecisionlog.command.app.port.ParkingLotPublisher;
import com.example.opa.policydecisionlog.shared.config.KafkaCustomProperties;
import com.example.opa.policydecisionlog.shared.metrics.DecisionLogMetrics;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Answers;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.support.Acknowledgment;
import tools.jackson.databind.json.JsonMapper;

import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.*;

@ExtendWith(MockitoExtension.class)
class ParkingLotConsumerTest {

    private ParkingLotConsumer consumer;

    @Mock
    private PersistDecisionLogUseCase persistDecisionLogUseCase;

    @Mock
    private ParkingLotPublisher parkingLotPublisher;

    @Mock
    private Acknowledgment ack;

    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private DecisionLogMetrics metrics;

    private JsonMapper jsonMapper;
    private KafkaCustomProperties properties;

    @BeforeEach
    void setUp() {
        jsonMapper = JsonMapper.builder().build();
        KafkaCustomProperties.ProducerSettings producerSettings =
                new KafkaCustomProperties.ProducerSettings(3, 30000, 10000, 5000);
        KafkaCustomProperties.ParkingRecoverySettings parkingRecovery =
                new KafkaCustomProperties.ParkingRecoverySettings(5, 60000, 2.0, 3600000);
        properties = new KafkaCustomProperties(
                "decision-logs", "decision-logs-dlq", "decision-logs-parking", "decision-logs-parking-dlq",
                producerSettings, producerSettings, producerSettings, parkingRecovery,
                new KafkaCustomProperties.ConsumerBackoff(1000, 2.0, 10000, 30000)
        );
        consumer = new ParkingLotConsumer(
                persistDecisionLogUseCase, parkingLotPublisher,
                jsonMapper, properties, metrics
        );
    }

    @Nested
    @DisplayName("consume - not-before 체크")
    class NotBeforeCheck {

        @Test
        @DisplayName("now < not-before 면 nack 호출")
        void givenNotBeforeInFuture_whenConsume_thenNack() {
            // given
            UUID decisionId = UUID.randomUUID();
            long notBefore = System.currentTimeMillis() + 60000; // 1분 후
            String payload = createPayload(decisionId);

            // when
            consumer.consume(payload, 0, 0L, 0, notBefore, ack);

            // then
            then(ack).should().nack(any(Duration.class));
            then(ack).should(never()).acknowledge();
            then(persistDecisionLogUseCase).shouldHaveNoInteractions();
        }

        @Test
        @DisplayName("now >= not-before 면 처리 진행")
        void givenNotBeforePassed_whenConsume_thenProcess() {
            // given
            UUID decisionId = UUID.randomUUID();
            long notBefore = System.currentTimeMillis() - 1000; // 1초 전
            String payload = createPayload(decisionId);

            given(persistDecisionLogUseCase.executeRecovery(any(), anyInt()))
                    .willReturn(PersistResult.SUCCESS);

            // when
            consumer.consume(payload, 0, 0L, 0, notBefore, ack);

            // then
            then(ack).should().acknowledge();
            then(persistDecisionLogUseCase).should().executeRecovery(any(), eq(0));
        }
    }

    @Nested
    @DisplayName("consume - 최대 재시도 횟수 체크")
    class MaxRetryCheck {

        @Test
        @DisplayName("attempt >= maxRetry 면 parking-dlq로 이동")
        void givenMaxRetryExceeded_whenConsume_thenSendToParkingDlq() {
            // given
            UUID decisionId = UUID.randomUUID();
            long notBefore = System.currentTimeMillis() - 1000;
            String payload = createPayload(decisionId);

            // when - maxRetry=5이므로 5는 초과
            consumer.consume(payload, 0, 0L, 5, notBefore, ack);

            // then
            then(parkingLotPublisher).should().toParkingDlq(any(DecisionLogIngestCommand.class));
            then(metrics).should().recordParkingDlqSent(1);
            then(ack).should().acknowledge();
            then(persistDecisionLogUseCase).shouldHaveNoInteractions();
        }

        @Test
        @DisplayName("attempt < maxRetry 면 정상 처리 시도")
        void givenRetryNotExceeded_whenConsume_thenTryProcess() {
            // given
            UUID decisionId = UUID.randomUUID();
            long notBefore = System.currentTimeMillis() - 1000;
            String payload = createPayload(decisionId);

            given(persistDecisionLogUseCase.executeRecovery(any(), anyInt()))
                    .willReturn(PersistResult.SUCCESS);

            // when
            consumer.consume(payload, 0, 0L, 2, notBefore, ack);

            // then
            then(parkingLotPublisher).should(never()).toParkingDlq(any());
            then(persistDecisionLogUseCase).should().executeRecovery(any(), eq(2));
        }
    }

    @Nested
    @DisplayName("consume - DB 저장")
    class DbSave {

        @Test
        @DisplayName("복구 성공 시 메트릭 기록하고 ack")
        void givenRecoverySuccess_whenConsume_thenRecordMetricsAndAck() {
            // given
            UUID decisionId = UUID.randomUUID();
            long notBefore = System.currentTimeMillis() - 1000;
            String payload = createPayload(decisionId);

            given(persistDecisionLogUseCase.executeRecovery(any(), anyInt()))
                    .willReturn(PersistResult.SUCCESS);

            // when
            consumer.consume(payload, 0, 0L, 0, notBefore, ack);

            // then
            then(persistDecisionLogUseCase).should().executeRecovery(any(), eq(0));
            then(metrics).should().recordParkingRecovered(1);
            then(ack).should().acknowledge();
        }

        @Test
        @DisplayName("복구 PARKED 반환 시 ack (재시도로 parking에 전송됨)")
        void givenRecoveryParked_whenConsume_thenAck() {
            // given
            UUID decisionId = UUID.randomUUID();
            long notBefore = System.currentTimeMillis() - 1000;
            String payload = createPayload(decisionId);

            given(persistDecisionLogUseCase.executeRecovery(any(), anyInt()))
                    .willReturn(PersistResult.PARKED);

            // when
            consumer.consume(payload, 0, 0L, 1, notBefore, ack);

            // then
            then(persistDecisionLogUseCase).should().executeRecovery(any(), eq(1));
            then(metrics).should(never()).recordParkingRecovered(anyInt());
            then(ack).should().acknowledge();
        }
    }

    private String createPayload(UUID decisionId) {
        DecisionLogIngestCommand command = new DecisionLogIngestCommand(
                decisionId,
                OffsetDateTime.now(),
                "cloud_access/policy/main",
                null, null, null, null, null, null, null
        );

        try {
            return jsonMapper.writeValueAsString(command);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
