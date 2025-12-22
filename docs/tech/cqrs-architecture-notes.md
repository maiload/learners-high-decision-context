# CQRS Architecture – Technical Notes

## 1. CQRS Pattern
- Command와 Query의 책임을 분리하여 각각 독립적으로 최적화 가능
- Command: 데이터 변경 (Create, Update, Delete)
- Query: 데이터 조회 (Read)
- 본 프로젝트에서는 동일 DB를 사용하되 코드 레벨에서 패키지를 분리하여 적용
  - `command/` - Decision Log 수집
  - `query/` - Decision Log 조회
- [CQRS Pattern - Microsoft](https://learn.microsoft.com/en-us/azure/architecture/patterns/cqrs)

## 2. Package Structure
- 레이어별 의존성 방향: `api` → `app` → `infra`
- command, query 는 독립적인 패키지 구조를 가짐
```
command/
- api/          # Controller, Request DTO, Mapper
- app/          # Service, Command DTO, Mapper
- infra/        # Repository, Entity

query/
- api/          # Controller, Response DTO, Mapper
- app/          # Service, Query DTO
- infra/        # Repository, Projection DTO
```

## 3. Model Separation
- Command와 Query가 동일 테이블을 참조하더라도 별도의 모델 사용
- Command 모델: `DecisionLog` (JPA Entity, 쓰기 최적화)
- Query 모델: `DecisionLogRow` (record DTO, 읽기 최적화)
- Query 모델은 `@Entity` 대신 QueryDSL Projection으로 직접 DTO 생성
- 이를 통해 Command 변경이 Query에 영향을 주지 않음

## 4. Layer Dependency Isolation
- Mapper를 각 레이어에 배치하여 의존성 역전 방지
  - `api/mapper/` - Request → Command/Query 변환
  - `app/mapper/` - Command → Entity 변환
- shared 패키지는 cross-cutting concern만 포함
  - `shared/config/` - 공통 설정 (QueryDslConfig 등)
  - `shared/exception/` - 공통 예외
  - `shared/api/` - GlobalExceptionHandler
- 레이어 간 순환 참조 방지를 위해 DTO는 해당 레이어에서만 정의
