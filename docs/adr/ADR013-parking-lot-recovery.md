# ADR013: Parking Lot Recovery Mechanism

### Status
Accepted

---

### Context
ADR009에서 인프라 에러(DB 연결 실패 등) 발생 시 레코드를 Parking Lot 토픽으로 전송하기로 결정했다.
Parking Lot에 보관된 레코드는 일정 시간 후 재처리되어야 하며,
이를 위한 별도의 Consumer와 복구 정책이 필요하다.

---

### Problem
Parking Lot 토픽에 전송된 레코드를 어떻게 복구할 것인가?

**고려 사항:**
- 즉시 재시도하면 동일한 인프라 에러가 반복될 가능성이 높음
- 무한 재시도는 리소스 낭비와 로그 폭증을 유발
- 너무 긴 대기 시간은 데이터 지연을 증가시킴
- 복구 불가능한 레코드는 별도 격리가 필요

---

### Alternatives

#### 1. 스케줄러 기반 배치 재처리
주기적으로 Parking Lot 토픽을 읽어 일괄 재처리

- 구현이 단순함
- 재처리 주기가 고정되어 유연성이 낮음
- 스케줄러 장애 시 복구 지연

#### 2. Kafka Consumer 기반 실시간 복구
별도 Consumer Group으로 Parking Lot 토픽을 구독하고,
메시지 헤더의 `not-before` 시각까지 대기 후 재처리

- 실시간에 가까운 복구 가능
- 개별 레코드별 백오프 적용 가능
- Consumer가 상시 동작해야 함

---

### Decision
대안 2를 선택

Parking Lot 토픽을 구독하는 별도 Consumer(`ParkingLotConsumer`)를 구현하고,
헤더 기반 지연 재처리와 지수 백오프를 적용한다.

#### 메시지 헤더

| Header | 설명 |
|--------|------|
| `x-retry-attempt` | 현재 재시도 횟수 (0부터 시작) |
| `x-not-before` | 이 시각 이전에는 처리하지 않음 (epoch millis) |

#### 복구 흐름

```
ParkingLotConsumer.consume()
│
├─ 1. NOT-BEFORE 체크
│     now < notBefore?
│     ├─ YES → ack.nack(waitMs) → 나중에 재전달
│     └─ NO  → 다음 단계
│
├─ 2. 메시지 파싱
│     파싱 실패?
│     ├─ YES → throw → DLQ로 전송
│     └─ NO  → 다음 단계
│
├─ 3. MAX RETRY 체크
│     attempt >= maxRetry?
│     ├─ YES → Parking DLQ로 전송 → ack
│     └─ NO  → 다음 단계
│
├─ 4. DB 저장 시도
│     persistDecisionLogUseCase.executeRecovery()
│     ├─ SUCCESS → 메트릭 기록 → ack
│     └─ PARKED  → ErrorHandler가 retry 또는 DLQ 처리 → ack
```

#### 지수 백오프 설정

```yaml
opa:
  kafka:
    parking-recovery:
      max-retry: 5
      initial-backoff-ms: 60000      # 1분
      multiplier: 2.0
      max-backoff-ms: 3600000        # 1시간
```

| Attempt | 대기 시간 |
|---------|----------|
| 0 | 1분 |
| 1 | 2분 |
| 2 | 4분 |
| 3 | 8분 |
| 4 | 16분 |
| 5+ | Parking DLQ로 이동 |

#### nack 기반 지연

Kafka Consumer의 `nack(Duration)`을 활용하여 not-before까지 대기:

```java
if (nowMillis < notBefore) {
    long waitMs = Math.min(notBefore - nowMillis, POLL_INTERVAL.toMillis());
    ack.nack(Duration.ofMillis(waitMs));
    return;
}
```

- `nack()`은 해당 레코드의 offset을 커밋하지 않고 대기 후 재전달
- 최대 대기 시간(30초)을 초과하면 다시 poll하여 not-before 재확인

#### Parking DLQ

최대 재시도 횟수 초과 시 `decision-logs-parking-dlq` 토픽으로 이동:

- 복구 불가능한 레코드 격리
- 수동 분석 및 조치 필요
- 별도 모니터링 알림 권장

#### 복구 시 에러 처리

`executeRecovery()` 실패 시 `ErrorHandler.handleRecovery()` 호출:

- Retryable 에러: `parkingLotPublisher.retry(command, attempt + 1)`
- Non-retryable 에러: `parkingLotPublisher.toDlq(command)`

---

### Consequences
- 인프라 장애 복구 후 Parking Lot 레코드가 자동으로 재처리됨
- 지수 백오프로 불필요한 재시도 부하 감소
- 최대 재시도 횟수로 무한 루프 방지
- nack 기반 지연으로 Kafka 리밸런싱 없이 대기 가능

트레이드 오프:
- Parking Lot Consumer가 상시 동작해야 함
- not-before 이전 레코드가 많으면 poll 오버헤드 발생
- Parking DLQ 레코드는 수동 처리 필요
- 복구 실패 시 attempt 증가에 따른 대기 시간 증가로 지연 발생 가능
