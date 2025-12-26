package com.example.opa.policydecisionlog.query.api.dto;

import com.example.opa.policydecisionlog.query.app.dto.DecisionContext;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Schema(description = "Decision Context Response - 단건 디버깅용 상세 컨텍스트")
public record DecisionContextResponse(
        @Schema(description = "요청 식별 + 결과")
        Request request,

        @Schema(description = "요청 주체/테넌트/플랫폼 정보 (컬럼 기반)")
        RequestInfo requestInfo,

        @Schema(description = "OPA 디버깅 메타 (컬럼 기반)")
        OpaInfo opa,

        @Schema(description = "디버깅 핵심 (raw 기반)")
        Decision decision,

        @Schema(description = "원본 조회 링크", example = "/decisions/{decisionId}")
        String rawLink
) {

    public static DecisionContextResponse from(DecisionContext context) {
        return new DecisionContextResponse(
                Request.from(context.request()),
                RequestInfo.from(context.requestInfo()),
                OpaInfo.from(context.opa()),
                Decision.from(context.decision()),
                context.rawLink()
        );
    }

    @Schema(description = "요청 식별 + 결과")
    public record Request(
            @Schema(description = "Decision ID")
            UUID decisionId,

            @Schema(description = "판단 시간")
            OffsetDateTime decidedAt,

            @Schema(description = "최종 허용 여부")
            boolean overallAllow,

            @Schema(description = "정책 경로", example = "cloud_access/device_posture/response")
            String path,

            @Schema(description = "서비스명", example = "cloud_access")
            String service
    ) {
        public static Request from(DecisionContext.Request request) {
            return new Request(
                    request.decisionId(),
                    request.decidedAt(),
                    request.overallAllow(),
                    request.path(),
                    request.service()
            );
        }
    }

    @Schema(description = "요청 정보")
    public record RequestInfo(
            @Schema(description = "요청자 IP", example = "192.168.1.100:12345")
            String requestedBy,

            @Schema(description = "요청 ID")
            Long reqId
    ) {
        public static RequestInfo from(DecisionContext.RequestInfo requestInfo) {
            return new RequestInfo(requestInfo.requestedBy(), requestInfo.reqId());
        }
    }

    @Schema(description = "OPA 디버깅 메타")
    public record OpaInfo(
            @Schema(description = "OPA 인스턴스 ID")
            UUID instanceId,

            @Schema(description = "OPA 버전", example = "1.0.0")
            String version,

            @Schema(description = "번들 정보")
            Map<String, Object> bundles
    ) {
        public static OpaInfo from(DecisionContext.OpaInfo opaInfo) {
            return new OpaInfo(opaInfo.instanceId(), opaInfo.version(), opaInfo.bundles());
        }
    }

    @Schema(description = "raw 파싱")
    public record Decision(
            @Schema(description = "거절 사유 목록 (가중치 순)")
            List<Reason> reasons,

            @Schema(description = "정책별 판단 결과")
            List<PolicyResult> policies
    ) {
        public static Decision from(DecisionContext.Decision decision) {
            return new Decision(
                    decision.reasons().stream().map(Reason::from).toList(),
                    decision.policies().stream().map(PolicyResult::from).toList()
            );
        }
    }

    @Schema(description = "거절 사유")
    public record Reason(
            @Schema(description = "규칙 정보")
            Rule rule,

            @Schema(description = "사유 메시지", example = "user not in allowed list")
            String message,

            @Schema(description = "가중치", example = "30")
            int weight
    ) {
        public static Reason from(DecisionContext.Reason reason) {
            return new Reason(Rule.from(reason.rule()), reason.message(), reason.weight());
        }
    }

    @Schema(description = "규칙 정보")
    public record Rule(
            @Schema(description = "정책 이름", example = "access_control")
            String policy,

            @Schema(description = "표현식", example = "all")
            String expression,

            @Schema(description = "규칙 이름", example = "soft_user")
            String name,

            @Schema(description = "규칙 타입", example = "required_software_macos")
            String type
    ) {
        public static Rule from(DecisionContext.Rule rule) {
            return new Rule(rule.policy(), rule.expression(), rule.name(), rule.type());
        }
    }

    @Schema(description = "정책별 판단 결과")
    public record PolicyResult(
            @Schema(description = "정책 이름", example = "access_control")
            String policy,

            @Schema(description = "허용 여부")
            boolean allow,

            @Schema(description = "위반 개수")
            int violationsCount
    ) {
        public static PolicyResult from(DecisionContext.PolicyResult policyResult) {
            return new PolicyResult(policyResult.policy(), policyResult.allow(), policyResult.violationsCount());
        }
    }
}
