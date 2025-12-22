package com.example.opa.policydecisionlog.query.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.OffsetDateTime;
import java.util.UUID;

@Schema(description = "Decision Log 검색 요청")
public record DecisionLogSearchRequest(
        @Schema(description = "검색 시작 시간", example = "2025-12-22T00:00:00Z")
        OffsetDateTime from,

        @Schema(description = "검색 종료 시간", example = "2025-12-22T23:59:59Z")
        OffsetDateTime to,

        @Schema(description = "허용 여부 필터", example = "true")
        Boolean allow,

        @Schema(description = "사용자 ID 필터")
        UUID userId,

        @Schema(description = "Realm ID 필터")
        UUID realmId,

        @Schema(description = "정책 경로 필터 (부분 일치)", example = "/policy/main")
        String path,

        @Schema(description = "조회 개수 (기본값: 20, 최대: 100)", example = "20")
        Integer limit,

        @Schema(description = "페이징 커서 (이전 응답의 nextCursor 값)")
        OffsetDateTime cursor
) {
}
