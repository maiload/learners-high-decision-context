# Architecture – Technical Notes

## 1. CQRS Pattern
- Command와 Query의 책임을 분리하여 각각 독립적으로 최적화 가능
- Command: 데이터 변경 (Create, Update, Delete)
- Query: 데이터 조회 (Read)
- 본 프로젝트에서는 동일 DB를 사용하되 코드 레벨에서 패키지를 분리하여 적용
  - `command/` - Decision Log 수집
  - `query/` - Decision Log 조회
- [CQRS Pattern - Microsoft](https://learn.microsoft.com/en-us/azure/architecture/patterns/cqrs)

## 2. Package Structure
- 레이어별 의존성 방향: `api` → `app` ← `infra`
- Repository 인터페이스는 `app/`에, 구현체는 `infra/`에 위치 (의존성 역전)
- command, query 는 독립적인 패키지 구조를 가짐
```
command/
- api/          # Controller, Request DTO, Mapper, Filter
- app/          # Service, Command DTO, Repository 인터페이스
- infra/        # Repository 구현체, Entity, Mapper

query/
- api/          # Controller, Response DTO, Mapper
- app/          # Service, Query DTO, Repository 인터페이스, ReadModel
- infra/        # Repository 구현체, Projection DTO, Mapper
```

## 3. Model Separation
- Command와 Query가 동일 테이블을 참조하더라도 별도의 모델 사용
- Command 모델: `DecisionLogEntity` (JPA Entity, 쓰기 최적화)
- Query 모델: `DecisionLogReadModel` (record DTO, 읽기 최적화)
- Query 모델은 `@Entity` 대신 QueryDSL Projection으로 직접 DTO 생성
- 이를 통해 Command 변경이 Query에 영향을 주지 않음

## 4. Hexagonal / Clean Architecture
- 헥사고날 아키텍처(Ports & Adapters)와 클린 아키텍처 원칙 적용
  - `api/` - Driving Adapter (In): 외부에서 애플리케이션을 호출 (Controller, Kafka Consumer)
  - `app/` - Application Core: 핵심 비즈니스 로직, UseCase, Port 정의
  - `infra/` - Driven Adapter (Out): 애플리케이션이 외부를 호출 (DB, Kafka, File)
- Port(인터페이스)는 app 레이어에, Adapter(구현체)는 api/infra 레이어에 위치

### Command Side Ports
```
command/app/port/
├── DecisionLogEventPublisher   # Kafka 원본 토픽 발행
├── DecisionLogPersistence      # DB 저장 (save, saveAll)
├── ParkingLotPublisher         # Parking Lot/DLQ 발행
└── InfrastructureFailureWriter # 장애 시 파일 기록
```

### Command Side Adapters
```
command/infra/
├── kafka/
│   ├── DecisionLogConsumer         # Driving Adapter (Main Topic)
│   ├── ParkingLotConsumer          # Driving Adapter (Parking Topic)
│   ├── DecisionLogEventPublisherImpl
│   └── ParkingLotPublisherImpl
├── db/
│   └── DecisionLogPersistenceImpl
└── file/
    └── InfrastructureFailureFileWriter
```

### Query Side Ports
```
query/app/
└── DecisionLogQueryRepository  # DB 조회 (인터페이스)
```

### Query Side Adapters
```
query/infra/
└── DecisionLogQueryRepositoryImpl
```

- Mapper를 각 레이어에 배치하여 의존성 방향 준수
  - `api/mapper/` - Request DTO → App DTO 변환
  - `infra/mapper/` - App DTO → Entity 변환, Projection → ReadModel 변환
- shared 패키지는 공통 관심사만 포함
  - `shared/config/` - Kafka, QueryDsl 등 공통 설정
  - `shared/exception/` - 공통 예외
  - `shared/metrics/` - Prometheus 메트릭
  - `shared/kafka/` - Kafka 헤더 상수
- 레이어 간 순환 참조 방지를 위해 DTO는 해당 레이어에서만 정의
- [Hexagonal Architecture](https://tech.osci.kr/hexagonal-architecture/)
- [Domain-Driven 헥사고날 아키텍처 - KakaoStyle](https://devblog.kakaostyle.com/ko/2025-03-21-1-domain-driven-hexagonal-architecture-by-example/)

## 5. Cursor-based Pagination
- Spring Data JPA의 `Page`/`Pageable` 대신 Cursor 기반 페이징 직접 구현
- `Page`/`Pageable` 미사용 이유:
  - 내부적으로 Offset 기반으로 동작
  - 전체 개수 조회를 위한 추가 COUNT 쿼리 발생
- Offset 방식의 문제점:
  - 페이지가 깊어질수록 성능 저하 (`OFFSET 10000`은 10000개를 스캔 후 스킵)
  - 데이터 추가/삭제 시 중복 또는 누락 발생 가능
- Cursor 방식의 장점:
  - 인덱스를 활용한 일정한 성능 (`WHERE ts < :cursor`)
  - 실시간 데이터 변경에도 일관된 결과
- `limit + 1` 패턴으로 다음 페이지 존재 여부 확인:
  - 요청: `limit = 20`
  - 조회: `LIMIT 21` (limit + 1)
  - 결과가 21개면 다음 페이지 존재, 20개만 반환하고 마지막 항목의 timestamp를 nextCursor로 제공
- `CursorPage<T>` 제네릭 DTO로 페이징 결과 표현

## 6. Strategy + Registry Pattern
- DecisionContext 추출 로직에 Strategy + Registry 패턴 적용
- **Strategy 패턴**: 서비스별로 다른 추출 전략을 동일한 인터페이스로 제공
  - `DecisionExtractor` - 추출 전략 인터페이스
  - `CloudAccessDecisionExtractor` - cloud_access 서비스 전용 전략
  - `DefaultDecisionExtractor` - 기본 fallback 전략
- **Registry 패턴**: 여러 Strategy를 보관하고 조건에 맞는 것을 조회
  - `DecisionExtractorRegistry` - Extractor 목록을 보관하고 서비스명으로 조회
  - Spring이 `List<DecisionExtractor>`를 자동 주입
  - 새로운 서비스 추가 시 `DecisionExtractor` 구현체만 추가하면 자동 등록

## 7. Kafka Event Streaming
- Decision Log 수집은 Kafka 기반 이벤트 스트리밍으로 처리
- HTTP API는 Kafka로 발행만 하고 즉시 응답 (비동기 처리)
- Kafka Consumer가 배치 단위로 소비하여 DB 저장

### Kafka Topics
| Topic | 용도 |
|-------|------|
| `decision-logs` | 원본 Decision Log |
| `decision-logs-parking` | 인프라 에러로 실패한 레코드 (지연 재시도) |
| `decision-logs-dlq` | 데이터 에러로 실패한 레코드 (수동 분석 필요) |
| `decision-logs-parking-dlq` | Parking Lot 최대 재시도 초과 |

### Producer 전략
- 용도별 KafkaTemplate 분리 (ADR011 참조)
  - `fastKafkaTemplate` - 원본 발행 (빠른 응답)
  - `parkingKafkaTemplate` - Parking Lot (중간 신뢰성)
  - `dlqKafkaTemplate` - DLQ (최대 신뢰성)

## 8. Error Handling Strategy
- 에러 유형에 따라 다른 처리 전략 적용 (ADR009 참조)

### 에러 분류 (ErrorClassifier)
- **Retryable**: SQLException의 SQLState 기반 판단
  - 08xxx (Connection), 40P01 (Deadlock), 55P03 (Lock), 57014 (Timeout)
  - → Parking Lot으로 이동, 지수 백오프로 재시도
- **Non-Retryable**: 데이터 에러, 파싱 에러
  - → Bisect 알고리즘으로 실패 레코드 식별 후 DLQ 이동

### Parking Lot 복구 (ADR013 참조)
- 지수 백오프: 1분 → 2분 → 4분 → 8분 → 16분
- 최대 5회 재시도 후 Parking DLQ로 이동
- `x-retry-attempt`, `x-not-before` 헤더로 상태 관리

### Composite Error Handler
```
CompositeErrorHandler
├── KafkaInfraException → infraErrorHandler → 파일 기록
└── 그 외 예외 → dlqErrorHandler → DLQ 전송
```

### Bisect 알고리즘
- 배치 내 일부 레코드만 실패할 때 정상 레코드는 살리고 실패 레코드만 격리
- 이진 탐색으로 O(log n) 복잡도로 실패 레코드 식별

## 9. Observability
- Prometheus + Grafana 기반 모니터링

### 주요 메트릭
| 메트릭 | 설명 |
|--------|------|
| `decision_log_consume_duration` | Kafka 배치 처리 시간 |
| `decision_log_e2e_latency` | 수집부터 저장까지 지연 시간 |
| `decision_log_db_save_total` | DB 저장 성공/실패 카운트 |
| `decision_log_parking_sent_total` | Parking Lot 전송 카운트 |
| `decision_log_dlq_sent_total` | DLQ 전송 카운트 |
| `decision_log_parking_recovered_total` | Parking Lot 복구 성공 카운트 |

### 인프라 구성
```
Application → Prometheus (:9090) → Grafana (:3000)
```
- `docker-compose.observability.yml`로 로컬 환경 구성
- `/actuator/prometheus` 엔드포인트로 메트릭 노출
