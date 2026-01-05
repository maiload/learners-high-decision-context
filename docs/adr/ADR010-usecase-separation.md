# ADR010: UseCase 분리

### Status
Accepted

---

### Context
Kafka 도입 이전에는 decision log 처리 흐름이 단순했다.

```
Controller → CommandService → Repository → DB
```

하지만 Kafka를 도입하면서 흐름이 두 갈래로 분리되었다.

1. **Publish 흐름**: HTTP 요청 → Kafka 발행
2. **Persist 흐름**: Kafka 소비 → DB 저장

기존 `DecisionLogCommandService`는 DB 저장만 담당했으나,
Controller는 Service를 거치지 않고 Producer를 직접 호출하는 구조가 되었다.

---

### Problem

**단일 서비스의 책임 모호**
- `CommandService`가 어떤 흐름에서 사용되는지 명확하지 않음
- Controller는 Service를 거치지 않고 Producer를 직접 호출
- 코드만 봐서는 전체 흐름을 파악하기 어려움

**흐름 간 일관성 부재**
- Publish 흐름: Controller → Producer (Service 없음)
- Persist 흐름: Consumer → CommandService → Repository
- 두 흐름의 레이어 구조가 다름

---

### Decision

**UseCase 분리**
```
command/app/
├── PublishDecisionLogUseCase.java   # Kafka 발행
├── PersistDecisionLogUseCase.java   # DB 저장
```

**Publish 흐름**
```
Controller → PublishDecisionLogUseCase → EventPublisher → Kafka
```

**Persist 흐름**
```
Consumer → PersistDecisionLogUseCase → Persistence → DB
```

---

### Consequences

**장점**
- 각 유스케이스의 책임이 명확하게 분리됨
- 새로운 유스케이스 추가 시 기존 코드 수정 없이 확장 가능
- 코드만 봐도 전체 흐름을 파악할 수 있음
- 향후 validation, 전처리, 로깅 등 로직 추가 시 적절한 위치가 됨

**트레이드 오프**
- 클래스 수가 증가함
- 현재 로직이 단순하여 UseCase가 얇은 위임 레이어처럼 보일 수 있음
