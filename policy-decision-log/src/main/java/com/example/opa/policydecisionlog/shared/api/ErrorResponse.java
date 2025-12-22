package com.example.opa.policydecisionlog.shared.api;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.OffsetDateTime;

@Schema(description = "에러 응답")
public record ErrorResponse(
        @Schema(description = "HTTP 상태 코드", example = "404")
        int status,

        @Schema(description = "에러 타입", example = "Not Found")
        String error,

        @Schema(description = "에러 메시지", example = "Decision not found: 550e8400-e29b-41d4-a716-446655440000")
        String message,

        @Schema(description = "요청 경로", example = "/decisions/550e8400-e29b-41d4-a716-446655440000")
        String path,

        @Schema(description = "발생 시간")
        OffsetDateTime timestamp
) {
    public static ErrorResponse of(int status, String error, String message, String path) {
        return new ErrorResponse(status, error, message, path, OffsetDateTime.now());
    }
}
