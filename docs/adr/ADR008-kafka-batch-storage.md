# ADR008: Kafka Consumer Batch Storage

### Status
Accepted

---

### Context
Kafka 기반으로 decision log 수집 파이프라인을 구성하면서,
DB 저장을 consumer가 담당하는 구조로 전환했다.

decision log는 대량 유입이 가능하고, DB 저장은 상대적으로 비용이 큰 작업이기 때문에
단건 INSERT 중심 구조는 다음 문제를 유발할 수 있다.

- DB round-trip 증가로 인한 저장 처리량 한계
- 인덱스 갱신 비용 누적에 따른 write 병목
- consumer 처리 지연 → lag 증가 → 운영 신뢰도 저하

또한 Kafka 소비 모델은 구조적으로 at-least-once 처리를 하기 때문에,
DB 저장 단계는 중복 처리에 안전(idempotent)해야 한다.

---

### Problem
Consumer가 레코드를 수신 즉시 단건으로 저장할 경우,

- DB 연결/트랜잭션/네트워크 왕복 비용이 반복되어 처리량이 급격히 감소
- 트래픽 증가 시 consumer lag이 빠르게 누적
- 장애/재시도 상황에서 동일 decision이 중복 저장될 가능성 증가

---

### Alternatives

#### 1. 단건 저장 유지
구현이 단순하지만, 처리량 한계와 lag 누적 문제가 빠르게 발생할 수 있음

#### 2. 배치 저장 도입
consumer가 레코드를 일정 기준으로 모아 한 번에 DB에 저장

- DB round-trip을 줄이고 처리량을 올릴 수 있음
- Kafka의 배치 소비 방식과도 잘 맞음
- 배치 크기/지연 시간(윈도우)에 대한 정책 필요

---

### Decision
대안 2를 선택

Kafka consumer의 DB 저장은 단건이 아닌 배치 저장으로 구현한다.

배치 플러시(flush) 조건:
- `max_batch_size` = 500
- `max_batch_window` = 100ms

레코드가 빠르게 유입될 때는 500건 단위로 저장 효율을 확보하고,
유입량이 적을 때는 100ms마다 저장하여 지연을 과도하게 늘리지 않는다.

중복 저장 방지:
- `decision_id`에 UNIQUE 제약 적용
- PostgreSQL의 `ON CONFLICT (decision_id) DO NOTHING` 사용

Offset commit은 배치 DB 저장 성공 이후에만 수행하여
유실을 방지하고, 중복은 idempotency로 흡수한다.

#### 구현 방식: NamedParameterJdbcTemplate

배치 INSERT와 `ON CONFLICT DO NOTHING`을 함께 사용하기 위해
JPA 대신 `NamedParameterJdbcTemplate.batchUpdate()`를 사용

JPA를 사용하지 않는 이유:
- `saveAll()` → 내부적으로 `persist()`/`merge()` 반복, `ON CONFLICT` 구문 사용 불가
- Native Query + `executeUpdate()` → for문 반복 시 매번 DB round-trip 발생, 배치 효과 없음

`NamedParameterJdbcTemplate.batchUpdate()`는 JDBC 레벨에서 배치 INSERT를 수행하며,
`ON CONFLICT DO NOTHING`과 함께 사용 가능하다.

---

### Consequences
- DB round-trip 감소로 저장 처리량 향상
- consumer lag 누적 완화 및 운영 안정성 개선
- 재시도/재처리 상황에서도 중복 저장이 발생하지 않는 idempotent 저장 보장
- Kafka 기반 구조 확장(consumer concurrency, partition 확장)에 대한 기반 확보

트레이드 오프:
- 배치 크기 및 윈도우는 트래픽 패턴과 DB 성능에 따라 조정 필요
- 배치 저장 실패 시 재시도/백오프 및 DLQ 전략이 후속으로 요구될 수 있음
