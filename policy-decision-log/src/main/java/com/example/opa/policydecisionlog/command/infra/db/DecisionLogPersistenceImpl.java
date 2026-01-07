package com.example.opa.policydecisionlog.command.infra.db;

import com.example.opa.policydecisionlog.command.app.port.DecisionLogPersistence;
import com.example.opa.policydecisionlog.command.app.dto.DecisionLogIngestCommand;
import com.example.opa.policydecisionlog.command.infra.db.mapper.CommandToEntityMapper;
import com.example.opa.policydecisionlog.command.infra.db.model.DecisionLogEntity;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.core.namedparam.SqlParameterSource;
import org.springframework.stereotype.Repository;
import tools.jackson.databind.json.JsonMapper;

import java.util.List;

@Repository
@RequiredArgsConstructor
public class DecisionLogPersistenceImpl implements DecisionLogPersistence {

    private final NamedParameterJdbcTemplate jdbcTemplate;
    private final CommandToEntityMapper mapper;
    private final JsonMapper jsonMapper;

    private static final String INSERT_SQL = """
            INSERT INTO decision_logs (
                decision_id, ts, path, overall_allow, requested_by, req_id,
                opa_instance_id, opa_version, service, bundles, raw, created_at
            ) VALUES (
                :decisionId, :ts, :path, :overallAllow, :requestedBy, :reqId,
                :opaInstanceId, :opaVersion, :service,
                CAST(:bundles AS jsonb), CAST(:raw AS jsonb), NOW()
            ) ON CONFLICT (decision_id) DO NOTHING
            """;

    @Override
    public void save(DecisionLogIngestCommand command) {
        DecisionLogEntity entity = mapper.toEntity(command);
        jdbcTemplate.update(INSERT_SQL, toParameterSource(entity));
    }

    @Override
    public void saveAll(List<DecisionLogIngestCommand> commands) {
        List<DecisionLogEntity> entities = commands.stream()
                .map(mapper::toEntity)
                .toList();

        SqlParameterSource[] batchParams = entities.stream()
                .map(this::toParameterSource)
                .toArray(SqlParameterSource[]::new);

        jdbcTemplate.batchUpdate(INSERT_SQL, batchParams);
    }

    private SqlParameterSource toParameterSource(DecisionLogEntity entity) {
        return new MapSqlParameterSource()
                .addValue("decisionId", entity.getDecisionId())
                .addValue("ts", entity.getTs())
                .addValue("path", entity.getPath())
                .addValue("overallAllow", entity.isOverallAllow())
                .addValue("requestedBy", entity.getRequestedBy())
                .addValue("reqId", entity.getReqId())
                .addValue("opaInstanceId", entity.getOpaInstanceId())
                .addValue("opaVersion", entity.getOpaVersion())
                .addValue("service", entity.getService())
                .addValue("bundles", toJsonString(entity.getBundles()))
                .addValue("raw", toJsonString(entity.getRaw()));
    }

    private String toJsonString(Object obj) {
        if (obj == null) {
            return null;
        }
        return jsonMapper.writeValueAsString(obj);
    }
}
