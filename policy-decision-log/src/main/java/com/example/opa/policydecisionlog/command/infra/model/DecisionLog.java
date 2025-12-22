package com.example.opa.policydecisionlog.command.infra.model;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.OffsetDateTime;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

@Entity
@Table(name = "decision_logs")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class DecisionLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "decision_id", nullable = false, unique = true)
    private UUID decisionId;

    @Column(name = "ts", nullable = false)
    private OffsetDateTime ts;

    @Column(name = "path", nullable = false)
    private String path;

    @Column(name = "overall_allow", nullable = false)
    private boolean overallAllow;

    @Column(name = "requested_by", columnDefinition = "text")
    private String requestedBy;

    @Column(name = "req_id")
    private Long reqId;

    @Column(name = "opa_instance_id")
    private UUID opaInstanceId;

    @Column(name = "opa_version", columnDefinition = "text")
    private String opaVersion;

    @Column(name = "realm_id")
    private UUID realmId;

    @Column(name = "user_id")
    private UUID userId;

    @Column(name = "user_policy_id")
    private UUID userPolicyId;

    @Column(name = "os_type")
    private String osType;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "bundles", columnDefinition = "jsonb")
    private Map<String, Object> bundles;

    @Column(name = "violation_count")
    private Integer violationCount;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "raw", columnDefinition = "jsonb")
    private Map<String, Object> raw;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    public static DecisionLog of(UUID decisionId, OffsetDateTime ts, String path, boolean overallAllow,
                                 String requestedBy, Long reqId, UUID opaInstanceId, String opaVersion,
                                 UUID realmId, UUID userId, UUID userPolicyId, String osType,
                                 Map<String, Object> bundles, Integer violationCount, Map<String, Object> raw) {
        DecisionLog entity = new DecisionLog();
        entity.decisionId = decisionId;
        entity.ts = ts;
        entity.path = path;
        entity.overallAllow = overallAllow;
        entity.requestedBy = requestedBy;
        entity.reqId = reqId;
        entity.opaInstanceId = opaInstanceId;
        entity.opaVersion = opaVersion;
        entity.realmId = realmId;
        entity.userId = userId;
        entity.userPolicyId = userPolicyId;
        entity.osType = osType;
        entity.bundles = bundles;
        entity.violationCount = violationCount;
        entity.raw = raw;
        return entity;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof DecisionLog that)) return false;
        return Objects.equals(decisionId, that.decisionId);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(decisionId);
    }
}
