# ADR006: Decision Log Schema


### Status
Accepted

### Context
요청 단위 디버깅을 위해 OPA decision log를 저장할 스키마가 필요

초기 단계에서는 정책/룰/위반을 모두 정규화하기보다는 원본 로그를 보존하고,
조회/필터에 필요한 최소 필드만 컬럼으로 분리해 빠르게 하나의 요청을 확인할 수 있도록 집중

---

### Decision
`decision_logs` 한 테이블을 메인:
- 원본 decision log는 `raw`(JSONB)에 저장
  - PostgreSQL에서는 대부분의 경우 jsonb가 json보다 성능과 확장성 측면에서 우수함
  - [JSON vs. JSONB in PostgreSQL: A Complete Comparison](https://www.dbvis.com/thetable/json-vs-jsonb-in-postgresql-a-complete-comparison/)
- 운영/디버깅에서 자주 쓰는 필드만 컬럼으로 분리

컨텍스트(Context)는 애플리케이션 레이어에서 생성하며,
아직 형태가 고정되지 않았기 때문에 DB에 별도 저장하는 방식은 추후에 결정
