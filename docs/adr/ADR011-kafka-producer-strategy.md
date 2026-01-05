# ADR011: Kafka Producer Strategy

### Status
Accepted

---

### Context
decision log 파이프라인에서 Kafka producer는 세 가지 용도로 사용된다.

1. **원본 발행**: HTTP로 수신한 decision log를 Kafka 토픽에 발행
2. **Parking Lot 발행**: 인프라 에러로 실패한 레코드를 재처리용 토픽에 발행
3. **DLQ 발행**: 데이터 에러로 실패한 레코드를 Dead Letter 토픽에 발행

각 용도는 요구되는 응답 속도, 재시도 허용 범위, 장애 대응 방식이 다르다.

---

### Problem
단일 producer 설정으로 모든 용도를 처리할 경우 다음 문제가 발생한다.

**원본 발행 관점**
- HTTP 응답 지연을 최소화해야 함
- producer 설정이 느슨하면 OPA → 수신 서버 간 latency가 증가
- 타임아웃이 길면 클라이언트 측 재시도가 겹쳐 중복 발행 가능성 증가

**에러 처리 관점**
- Parking Lot / DLQ 발행은 원본보다 신뢰성이 더 중요
- 짧은 타임아웃 설정 시 에러 레코드가 유실될 수 있음
- 재시도 횟수를 늘려서라도 전송 성공률을 높여야 함

단일 설정으로는 "빠른 응답"과 "높은 신뢰성"을 동시에 만족하기 어렵다.

---

### Alternatives

#### 1. 단일 Producer 설정 유지
모든 용도에 동일한 timeout/retry 설정 적용

- 구현이 단순함
- 원본 발행이 느려지거나, 에러 처리가 불안정해지는 트레이드오프 발생

#### 2. 용도별 Producer 분리
각 용도에 맞는 별도 `KafkaTemplate` 빈을 구성

- 원본 발행: 빠른 응답 우선
- Parking Lot: 중간 수준의 신뢰성
- DLQ: 최대 신뢰성

---

### Decision
대안 2를 선택

용도별로 별도의 `ProducerFactory`와 `KafkaTemplate`을 구성하여
각각의 요구사항에 맞는 설정을 적용한다.

#### Producer 구성

| Producer | 용도 | retries | delivery.timeout.ms | request.timeout.ms | max.block.ms |
|----------|------|---------|---------------------|-------------------|--------------|
| fastKafkaTemplate | 원본 발행 | 1 | 8,000 | 3,000 | 2,000 |
| parkingKafkaTemplate | Parking Lot | 2 | 20,000 | 8,000 | 5,000 |
| dlqKafkaTemplate | DLQ | 3 | 30,000 | 12,000 | 5,000 |

#### 설정 근거

**fastKafkaTemplate (원본 발행)**
- HTTP 응답 지연을 최소화하기 위해 짧은 타임아웃
- 재시도 1회로 제한하여 중복 발행 가능성 감소
- 실패 시 클라이언트(OPA)가 재전송하므로 유실 위험 낮음

**parkingKafkaTemplate (Parking Lot)**
- 인프라 에러로 실패한 레코드는 나중에 재처리해야 함
- 원본보다 긴 타임아웃으로 전송 성공률 향상
- Consumer 스레드 블로킹을 과도하게 늘리지 않는 선에서 설정

**dlqKafkaTemplate (DLQ)**
- 데이터 에러 레코드는 DLQ에 도달하지 못하면 유실됨
- 최대한 긴 타임아웃과 재시도로 신뢰성 확보
- DLQ 발행 실패 시 폴백으로 파일 기록 (ADR012 참조)

#### 동기 전송 (get with timeout)

모든 producer는 `send().get(timeout, TimeUnit.MILLISECONDS)` 패턴으로 동기 전송한다.

```java
kafkaTemplate.send(topic, key, payload)
    .get(properties.fastProducer().getTimeoutMs(), TimeUnit.MILLISECONDS);
```

- 전송 성공/실패를 명시적으로 확인
- 실패 시 즉시 예외 발생으로 후속 처리(폴백, 로깅) 가능
- `getTimeoutMs()`는 `deliveryTimeoutMs - 1000ms` 마진을 적용

---

### Consequences
- 원본 발행은 빠른 응답을 유지하면서, 에러 처리는 높은 신뢰성을 확보
- 각 용도에 맞는 설정을 독립적으로 튜닝 가능
- 운영 중 특정 producer만 설정 변경 가능 (예: DLQ 브로커 분리 시)

트레이드 오프:
- Producer 빈이 3개로 증가하여 설정 관리 포인트 증가
- 각 producer의 커넥션 풀이 별도로 유지되어 리소스 사용량 증가
- 설정값은 트래픽 패턴과 브로커 상태에 따라 조정 필요
