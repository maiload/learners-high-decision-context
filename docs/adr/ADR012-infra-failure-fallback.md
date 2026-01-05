# ADR012: Infrastructure Failure Fallback

### Status
Accepted

---

### Context
decision log consumer는 에러 발생 시 DLQ 또는 Parking Lot 토픽으로 레코드를 전송한다.
하지만 Kafka 브로커 자체에 장애가 발생하면 이 전송도 실패할 수 있다.

- 브로커 다운
- 네트워크 단절
- 디스크 풀
- 클러스터 전체 장애

이 경우 에러 레코드를 전송할 곳이 없어 유실될 위험이 있다.
consumer가 무한 재시도하면 lag이 누적되고, 재시도를 포기하면 레코드가 사라진다.

---

### Problem
Kafka 기반 에러 처리(DLQ, Parking Lot)는 Kafka가 정상 동작한다는 전제 하에 작동한다.

Kafka 장애 시:
- DLQ 전송 실패 → 데이터 에러 레코드 유실
- Parking Lot 전송 실패 → 인프라 에러 레코드 유실
- 재시도 소진 후 레코드를 처리할 방법 없음

Kafka 의존성을 완전히 제거할 수는 없지만,
최소한의 폴백 메커니즘으로 레코드 유실을 방지해야 한다.

---

### Alternatives

#### 1. 폴백 없이 로그만 남기기
재시도 소진 후 에러 로그만 기록하고 레코드 버림

- 구현이 가장 단순
- 레코드 유실 발생
- 장애 복구 후 수동 복구 불가능

#### 2. 메모리 큐에 임시 저장
실패한 레코드를 메모리에 보관하고 주기적으로 재전송 시도

- 프로세스 재시작 시 메모리 큐 유실
- 장애가 길어지면 메모리 부족 위험

#### 3. 로컬 파일에 기록
실패한 레코드를 로컬 파일 시스템에 append-only로 기록

- 프로세스 재시작에도 데이터 보존
- 장애 복구 후 파일을 읽어 수동/자동 재처리 가능
- 디스크 공간 관리 필요

---

### Decision
대안 3을 선택

Kafka 에러 핸들러에서 재시도가 소진된 레코드는
로컬 파일 시스템에 JSONL(JSON Lines) 형식으로 기록한다.

#### 구현 구조

```
infraErrorHandler (DefaultErrorHandler)
    └── ConsumerRecordRecoverer
            └── InfrastructureFailureFileWriter
                    └── ./logs/infra-failures/infra-failure-{date}.jsonl
```

#### 파일 형식

- 형식: JSONL (한 줄에 하나의 JSON 객체)
- 파일명: `infra-failure-{yyyy-MM-dd}.jsonl`
- 경로: `${opa.infra-failure.path}` (기본값: `./logs/infra-failures`)

#### 기록 내용

```json
{
  "topic": "decision-logs",
  "partition": 0,
  "offset": 12345,
  "key": "decision-id-uuid",
  "value": "{...원본 메시지...}",
  "errorMessage": "Connection refused",
  "failedAt": "2025-01-05T10:30:00+09:00"
}
```

#### Append-Only 쓰기

- 레코드 단위로 즉시 flush
- 동시 쓰기 시에도 라인 단위 원자성 보장 (POSIX 기준)
- 날짜별 파일 분리로 로테이션 자동 처리

#### 에러 핸들러 분기

Spring Kafka의 `CommonErrorHandler`를 커스텀 구현하여 예외 유형에 따라 분기:

- `BatchListenerFailedException`: dlqErrorHandler → DLQ 전송
- 그 외 예외: infraErrorHandler → 파일 기록

---

### Consequences
- Kafka 브로커 장애 시에도 에러 레코드가 유실되지 않음
- 장애 복구 후 파일을 읽어 Kafka로 재발행하거나 DB에 직접 적재 가능
- 파일 기반이므로 모니터링 도구(파일 워처, 로그 수집기)와 연동 용이
- 레코드 단위 append로 부분 실패에도 이전 기록 보존

트레이드 오프:
- 로컬 디스크 공간 관리 필요 (장애가 길어지면 파일 크기 증가)
- 파일 재처리 로직은 별도 구현 필요 (수동 스크립트 또는 배치 잡)
- 파일 쓰기 자체가 실패하면 최종적으로 유실 (디스크 풀, 권한 오류 등)
- 다중 인스턴스 환경에서는 각 인스턴스별로 파일이 생성됨
