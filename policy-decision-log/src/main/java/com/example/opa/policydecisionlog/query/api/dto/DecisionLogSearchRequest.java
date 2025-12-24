package com.example.opa.policydecisionlog.query.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.OffsetDateTime;

@Schema(description = "Decision Log 검색 요청")
public record DecisionLogSearchRequest(
        @Schema(description = "검색 시작 시간", example = "2025-01-01T00:00:00Z")
        OffsetDateTime from,

        @Schema(description = "검색 종료 시간", example = "2025-12-31T23:59:59Z")
        OffsetDateTime to,

        @Schema(description = "허용 여부 필터", example = "true")
        Boolean allow,

        @Schema(description = "서비스명 필터", example = "cloud_access")
        String service,

        @Schema(description = "정책 경로 필터 (부분 일치)", example = "device_posture/response")
        String path,

        @Schema(description = "조회 개수 (기본값: 20, 최대: 100)", example = "20")
        Integer limit,

        @Schema(description = "페이징 커서 (이전 응답의 nextCursor 값)")
        OffsetDateTime cursor
) {
}
