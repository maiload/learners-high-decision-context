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

        @Schema(description = "정책 경로", example = "cloud_access/device_posture/response")
        String path,

        @Schema(description = "전체 허용 여부")
        boolean overallAllow,

        @Schema(description = "요청자 IP", example = "192.168.1.100:12345")
        String requestedBy,

        @Schema(description = "요청 ID")
        Long reqId,

        @Schema(description = "OPA 인스턴스 ID")
        UUID opaInstanceId,

        @Schema(description = "OPA 버전", example = "1.0.0")
        String opaVersion,

        @Schema(description = "서비스명", example = "cloud_access")
        String service,

        @Schema(description = "번들 정보")
        Map<String, Object> bundles,

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
                row.service(),
                row.bundles(),
                row.raw(),
                row.createdAt()
        );
    }
}
