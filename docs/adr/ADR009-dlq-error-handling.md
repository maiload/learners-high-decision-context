# ADR009: DLQ Error Handling

### Status
Accepted (Revised)

---

### Context
Kafka consumer는 decision log를 배치 단위로 수신하고,
DB에는 배치 저장을 통해 처리량을 확보하는 구조를 적용했다.

이 구조는 정상 상황에서는 효율적이지만,
배치 내부에 "항상 실패하는 레코드"가 포함될 경우 다음 문제가 발생할 수 있다.

- 배치 처리 중 예외 발생 → offset ack 불가
- 동일 배치가 반복 재처리됨
- consumer lag이 증가하고 파이프라인이 사실상 정지

또한 DB 저장은 외부 의존성이므로 일시적인 장애가 발생할 수 있으며,
이 경우에도 파이프라인 전체가 즉시 멈추기보다는 제한된 재시도로 회복 가능성을 확보할 필요가 있다.

---

### Problem
배치 리스너는 한 번의 poll에서 여러 레코드를 함께 처리한다.
이때 두 유형의 실패가 존재한다.

**데이터 실패 (영구 실패)**
- JSON 파싱 실패
- 스키마 불일치 / 필수 필드 누락
- 타입 변환 불가
- → 재처리해도 성공할 가능성이 낮음

**인프라 실패 (일시 실패)**
- DB 타임아웃, 커넥션 오류, 일시적인 락/부하
- → 재시도 시 성공할 가능성이 있음

기존처럼 "예외 발생 시 배치 전체 재처리"로만 대응하면
데이터 실패 1건 때문에 파이프라인 전체가 무한 재처리 상태에 빠질 수 있다.

---

### Decision

Spring Kafka의 `DefaultErrorHandler`와 `DeadLetterPublishingRecoverer`를 활용하여
에러 유형별로 분기 처리하는 Composite Error Handler 패턴을 적용한다.

#### 에러 핸들러 구조

```
compositeErrorHandler
├── infraErrorHandler (KafkaInfraException)
│   └── InfrastructureFailureWriter → 파일 기록
└── dlqErrorHandler (그 외 예외)
    └── DeadLetterPublishingRecoverer → DLQ 토픽
```

#### 파싱 실패
- Consumer에서 JSON 파싱 실패 시 `BatchListenerFailedException` 발생
- Spring Kafka가 해당 레코드만 추출하여 DLQ로 전송
- 나머지 레코드는 정상 처리

#### DB 저장 실패
- 최대 2회까지 재시도 (백오프: 1s → 2s)
- 2회 재시도에도 실패 시 ErrorHandler로 위임

#### ErrorHandler 처리
- [PostgreSQL Error Codes](https://www.postgresql.org/docs/17/errcodes-appendix.html) 기반으로 Retryable 여부 판단
- Retryable (인프라 에러): Parking Lot 토픽으로 전송
  - 08xxx: Connection exception
  - 40P01: Deadlock detected
  - 55P03: Lock not available
  - 57014: Query canceled (timeout)
  - 53300: Too many connections
  - 57Pxx: Admin/crash shutdown
- Non-retryable (데이터 에러): Bisect로 문제 레코드 탐색 후 DLQ 직접 전송
  - 배치를 반씩 나눠가며 실패 레코드만 격리
  - 정상 레코드는 저장 성공
  - 실패 레코드는 `parkingLotPublisher.toDlq()`로 직접 DLQ 전송

#### Kafka 에러 핸들러 (Composite)
- `KafkaInfraException` 발생 시: `infraErrorHandler` 처리
  - Kafka 자체 장애로 parking/DLQ 발행 실패 시 발생
  - 재시도 소진 후 파일로 기록 (ADR012 참조)
- 그 외 예외 발생 시: `dlqErrorHandler` 처리
  - 파싱 에러, 데이터 에러 등
  - 해당 레코드만 DLQ로 전송

#### Offset ack
- `AckMode.MANUAL` 사용
- 모든 에러 처리가 완료된 후 명시적으로 ack
- 에러 레코드는 DLQ 또는 파일로 이동하므로 유실되지 않음

#### Kafka 토픽
- 원본 토픽: `decision-logs`
- DLQ 토픽: `decision-logs-dlq`
- Parking Lot 토픽: `decision-logs-parking`
- Parking DLQ 토픽: `decision-logs-parking-dlq`

#### DLQ 메시지 포함 정보
- 원본 raw 메시지
- 실패 원인 (예외 타입/메시지)
- 원본 토픽/파티션/오프셋
- 처리 시각

---

### Consequences
- 데이터 불량 1건이 포함되더라도 배치 consumer가 무한 재처리에 빠지지 않음
- Spring Kafka의 표준 에러 핸들링 메커니즘을 활용하여 구현 복잡도 감소
- 인프라 에러는 Parking Lot에서 나중에 재시도 가능
- 데이터 에러는 Bisect로 정상 레코드를 최대한 살리고 문제 레코드만 DLQ로 격리
- 파이프라인이 멈추지 않고 지속적으로 전진하며 consumer lag을 안정적으로 관리 가능

트레이드 오프:
- Kafka 자체 장애 시에는 DLQ 전송도 실패할 수 있음 (ADR012에서 폴백 처리)
- Parking Lot 복구 처리는 별도 Consumer로 구현 (ADR013 참조)
- DLQ/Parking Lot 메시지의 보관(retention), 모니터링 절차는 운영 단계에서 정의 필요
