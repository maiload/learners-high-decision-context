package com.example.opa.policydecisionlog.command.infra.kafka;

import com.example.opa.policydecisionlog.command.app.PersistDecisionLogUseCase;
import com.example.opa.policydecisionlog.command.app.dto.PersistResult;
import com.example.opa.policydecisionlog.command.app.error.DataErrorException;
import com.example.opa.policydecisionlog.command.infra.kafka.exception.ConsumerProcessingException;
import com.example.opa.policydecisionlog.shared.metrics.DecisionLogMetrics;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Answers;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.listener.BatchListenerFailedException;
import org.springframework.kafka.support.Acknowledgment;
import tools.jackson.databind.json.JsonMapper;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.BDDMockito.*;

@ExtendWith(MockitoExtension.class)
class DecisionLogConsumerTest {

    private DecisionLogConsumer consumer;

    @Mock
    private PersistDecisionLogUseCase persistDecisionLogUseCase;

    @Mock
    private Acknowledgment acknowledgment;

    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private DecisionLogMetrics metrics;

    @BeforeEach
    void setUp() {
        JsonMapper jsonMapper = JsonMapper.builder().build();
        consumer = new DecisionLogConsumer(persistDecisionLogUseCase, jsonMapper, metrics);
    }

    @Nested
    @DisplayName("consume")
    class Consume {

        @Test
        @DisplayName("정상 파싱 및 저장 시 ack 호출")
        void givenValidRecords_whenConsume_thenAcknowledges() {
            // given
            List<ConsumerRecord<String, String>> records = createConsumerRecords();

            given(persistDecisionLogUseCase.execute(anyList())).willReturn(PersistResult.SUCCESS);

            // when
            consumer.consume(records, acknowledgment);

            // then
            then(persistDecisionLogUseCase).should().execute(anyList());
            then(acknowledgment).should().acknowledge();
        }

        @Test
        @DisplayName("빈 레코드 목록이면 persist 호출 없이 ack")
        void givenEmptyRecords_whenConsume_thenAcknowledgesWithoutPersist() {
            // given
            List<ConsumerRecord<String, String>> records = List.of();

            // when
            consumer.consume(records, acknowledgment);

            // then
            then(persistDecisionLogUseCase).shouldHaveNoInteractions();
            then(acknowledgment).should().acknowledge();
        }

        @Test
        @DisplayName("파싱 실패 시 BatchListenerFailedException 발생")
        void givenInvalidJson_whenConsume_thenThrowsBatchListenerFailedException() {
            // given
            String invalidJson = "{ invalid json }";
            ConsumerRecord<String, String> consumerRecord = new ConsumerRecord<>("decision-logs", 0, 123L, "key", invalidJson);
            List<ConsumerRecord<String, String>> records = List.of(consumerRecord);

            // when & then
            assertThatThrownBy(() -> consumer.consume(records, acknowledgment))
                    .isInstanceOf(BatchListenerFailedException.class)
                    .hasMessageContaining("Parse failed");

            then(acknowledgment).shouldHaveNoInteractions();
        }

        @Test
        @DisplayName("persist FAILED 반환 시 ConsumerProcessingException 발생")
        void givenPersistFails_whenConsume_thenThrowsConsumerProcessingException() {
            // given
            List<ConsumerRecord<String, String>> records = createConsumerRecords();

            given(persistDecisionLogUseCase.execute(anyList())).willReturn(PersistResult.FAILED);

            // when & then
            assertThatThrownBy(() -> consumer.consume(records, acknowledgment))
                    .isInstanceOf(ConsumerProcessingException.class)
                    .hasMessageContaining("Persist failed");

            then(acknowledgment).shouldHaveNoInteractions();
        }

        @Test
        @DisplayName("DataErrorException 발생 시 해당 레코드로 BatchListenerFailedException 발생")
        void givenDataError_whenConsume_thenThrowsBatchListenerFailedExceptionWithRecord() {
            // given
            List<ConsumerRecord<String, String>> records = createConsumerRecords();

            DataErrorException dataError = new DataErrorException(1, new RuntimeException("Data error"));
            given(persistDecisionLogUseCase.execute(anyList())).willThrow(dataError);

            // when & then
            assertThatThrownBy(() -> consumer.consume(records, acknowledgment))
                    .isInstanceOf(BatchListenerFailedException.class)
                    .hasMessageContaining("Data error")
                    .extracting(e -> ((BatchListenerFailedException) e).getRecord())
                    .isEqualTo(records.get(1));

            then(acknowledgment).shouldHaveNoInteractions();
        }

        @Test
        @DisplayName("persist PARKED 반환 시 정상 ack")
        void givenPersistParked_whenConsume_thenAcknowledges() {
            // given
            List<ConsumerRecord<String, String>> records = createConsumerRecords();

            given(persistDecisionLogUseCase.execute(anyList())).willReturn(PersistResult.PARKED);

            // when
            consumer.consume(records, acknowledgment);

            // then
            then(acknowledgment).should().acknowledge();
        }
    }

    private static List<ConsumerRecord<String, String>> createConsumerRecords() {
        UUID decisionId1 = UUID.randomUUID();
        UUID decisionId2 = UUID.randomUUID();
        String json1 = """
                    {"decision_id": "%s", "timestamp": "2026-01-05T10:00:00Z", "path": "cloud_access/device_posture/response"}
                    """.formatted(decisionId1);
        String json2 = """
                    {"decision_id": "%s", "timestamp": "2026-01-05T10:01:00Z", "path": "cloud_access/policy/main"}
                    """.formatted(decisionId2);

        ConsumerRecord<String, String> record1 = new ConsumerRecord<>("decision-logs", 0, 0L, decisionId1.toString(), json1);
        ConsumerRecord<String, String> record2 = new ConsumerRecord<>("decision-logs", 0, 1L, decisionId2.toString(), json2);
        return List.of(record1, record2);
    }
}
