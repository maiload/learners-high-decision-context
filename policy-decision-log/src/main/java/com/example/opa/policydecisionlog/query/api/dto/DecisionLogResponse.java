package com.example.opa.policydecisionlog.query.api.dto;

import com.example.opa.policydecisionlog.query.infra.model.DecisionLogRow;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.OffsetDateTime;
import java.util.Map;
import java.util.UUID;

@Schema(description = "Decision Log 응답")
public record DecisionLogResponse(
        @Schema(description = "Decision ID")
        UUID decisionId,

        @Schema(description = "판단 시간")
        OffsetDateTime timestamp,

        @Schema(description = "정책 경로", example = "/policy/main")
        String path,

        @Schema(description = "전체 허용 여부")
        boolean allow,

        @Schema(description = "요청자", example = "user@example.com")
        String requestedBy,

        @Schema(description = "요청 ID")
        Long reqId,

        @Schema(description = "OPA 인스턴스 ID")
        UUID opaInstanceId,

        @Schema(description = "OPA 버전", example = "1.0.0")
        String opaVersion,

        @Schema(description = "Realm ID")
        UUID realmId,

        @Schema(description = "사용자 ID")
        UUID userId,

        @Schema(description = "사용자 정책 ID")
        UUID userPolicyId,

        @Schema(description = "OS 타입", example = "WINDOWS")
        String osType,

        @Schema(description = "번들 정보")
        Map<String, Object> bundles,

        @Schema(description = "위반 개수", example = "2")
        Integer violationCount,

        @Schema(description = "원본 Decision Log (JSON)")
        Map<String, Object> raw,

        @Schema(description = "생성 시간")
        OffsetDateTime createdAt
) {
    public static DecisionLogResponse from(DecisionLogRow row) {
        return new DecisionLogResponse(
                row.decisionId(),
                row.ts(),
                row.path(),
                row.overallAllow(),
                row.requestedBy(),
                row.reqId(),
                row.opaInstanceId(),
                row.opaVersion(),
                row.realmId(),
                row.userId(),
                row.userPolicyId(),
                row.osType(),
                row.bundles(),
                row.violationCount(),
                row.raw(),
                row.createdAt()
        );
    }
}
