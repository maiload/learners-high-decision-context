# Decision Log Ingestion – Technical Notes

## 1. Spring Boot 4 and Jackson 3
- Spring Boot 4부터 JSON 처리의 기본 스택은 Jackson 3를 기준으로 구성
  - spring-boot-starter-webmvc 하위 의존성에서 Jackson 3 기반 JSON 스택을 사용
  - Jackson 3는 패키지 네임이 tools.jackson.*로 변경됨
- Jackson 관련 어노테이션(@JsonProperty, @JsonIgnore 등)은 기존과 동일하게 사용 가능
- JacksonAutoConfiguration에서 기존의 ObjectMapper 대신 JsonMapper 빈을 기본으로 등록
- HTTP 요청/응답 JSON 변환을 위해 기존 MappingJackson2HttpMessageConverter 대신 Jackson 3 기반 JacksonJsonHttpMessageConverter를 사용
- [MIGRATING_TO_JACKSON](https://github.com/FasterXML/jackson/blob/main/jackson3/MIGRATING_TO_JACKSON_3.md)
- [Introducing Jackson 3 support in Spring](https://spring.io/blog/2025/10/07/introducing-jackson-3-support-in-spring)
- [HTTP Message Conversion](https://docs.spring.io/spring-framework/reference/web/webmvc/message-converters.html)

## 2. Hibernate 7 JSONB mapping
- spring-boot-starter-data-jpa:4.0.0 기준으로 hibernate-core:7.1.8.Final이 사용
- Hibernate 7 공식 문서에서는 JSON 타입 매핑 시  Map<String, Object> 또는 기본 Java 타입 기반 구조를 권장
- 컨트롤러 / API 계층에서는 JSON 트리 탐색이 용이한 JsonNode를 사용하고 엔티티 저장 시에는 Map<String, Object>로 변환
- [Hibernate ORM User Guide - JSON mapping](https://docs.hibernate.org/orm/7.1/userguide/html_single/#basic-mapping-json)

## 3. Gzip decompression and size limits
- OPA Decision Log는 기본적으로 batch 단위로 gzip 압축되어 전송
- OPA 설정을 통해 압축된 payload의 최대 크기는 제한 가능 (default: 32KB)
- 압축률은 데이터 특성에 따라 달라지며 압축 해제 후(JSON 원문)의 크기는 OPA에서 직접 제한 불가능
- Baeldung의 JSON 압축 실험 결과를 참고하면,
Jackson 기본 설정 기준 JSON 데이터는 대략 70~80% 수준으로 압축됨을 확인 가능
- 이를 바탕으로 압축된 payload 기준 최대 크기를 약 1MB 수준으로 설정
- [Reducing JSON Data Size](https://www.baeldung.com/json-reduce-data-size#bd-conclusion)