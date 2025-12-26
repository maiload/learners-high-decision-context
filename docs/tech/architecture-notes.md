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
  - `api/` - Adapter (In Adapter): 외부에서 애플리케이션을 호출 (Controller)
  - `app/` - Domain (Hexagon): 핵심 비즈니스 로직, Port 정의
  - `infra/` - Adapter (Out Adapter): 애플리케이션이 외부를 호출 (DB, 외부 API)
- Port(인터페이스)는 app 레이어에, Adapter(구현체)는 api/infra 레이어에 위치
  - Outbound Port: `app/DecisionLogQueryRepository`, `app/DecisionLogCommandRepository`
  - Out Adapter: `infra/DecisionLogQueryRepositoryImpl`, `infra/DecisionLogCommandRepositoryImpl`
- Mapper를 각 레이어에 배치하여 의존성 방향 준수
  - `api/mapper/` - Request DTO → App DTO 변환
  - `infra/mapper/` - App DTO → Entity 변환, Projection → ReadModel 변환
- shared 패키지는 공통 관심사만 포함
  - `shared/config/` - 공통 설정 (QueryDslConfig 등)
  - `shared/exception/` - 공통 예외
  - `shared/api/` - GlobalExceptionHandler
- 레이어 간 순환 참조 방지를 위해 DTO는 해당 레이어에서만 정의
- [Hexagonal Architecture](https://tech.osci.kr/hexagonal-architecture/)
- [Domain-Driven 헥사고날 아키텍처 - KakaoStyle](https://devblog.kakaostyle.com/ko/2025-03-21-1-domain-driven-hexagonal-architecture-by-example/)
