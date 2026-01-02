package com.example.opa.policydecisionlog.query.infra;

import com.example.opa.policydecisionlog.query.app.DecisionLogQueryRepository;
import com.example.opa.policydecisionlog.query.app.dto.DecisionLogReadModel;
import com.example.opa.policydecisionlog.query.app.dto.DecisionLogSearchQuery;
import com.example.opa.policydecisionlog.query.infra.mapper.RowToReadModelMapper;
import com.example.opa.policydecisionlog.query.infra.model.DecisionLogRow;
import com.querydsl.core.types.ConstructorExpression;
import com.querydsl.core.types.Projections;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import org.springframework.util.StringUtils;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static com.example.opa.policydecisionlog.command.infra.db.model.QDecisionLogEntity.decisionLogEntity;

@Repository
@RequiredArgsConstructor
public class DecisionLogQueryRepositoryImpl implements DecisionLogQueryRepository {

    private final JPAQueryFactory queryFactory;
    private final RowToReadModelMapper mapper;

    @Override
    public Optional<DecisionLogReadModel> findByDecisionId(UUID decisionId) {
        DecisionLogRow result = queryFactory
                .select(projection())
                .from(decisionLogEntity)
                .where(decisionLogEntity.decisionId.eq(decisionId))
                .fetchOne();

        return Optional.ofNullable(result).map(mapper::toReadModel);
    }

    @Override
    public List<DecisionLogReadModel> search(DecisionLogSearchQuery query) {
        return queryFactory
                .select(projection())
                .from(decisionLogEntity)
                .where(
                        cursorCondition(query.cursor()),
                        timestampFrom(query.from()),
                        timestampTo(query.to()),
                        allowEquals(query.allow()),
                        serviceEquals(query.service()),
                        pathContains(query.path())
                )
                .orderBy(decisionLogEntity.ts.desc())
                .limit(query.limit() + 1L)
                .fetch()
                .stream()
                .map(mapper::toReadModel)
                .toList();
    }

    private ConstructorExpression<DecisionLogRow> projection() {
        return Projections.constructor(
                DecisionLogRow.class,
                decisionLogEntity.id,
                decisionLogEntity.decisionId,
                decisionLogEntity.ts,
                decisionLogEntity.path,
                decisionLogEntity.overallAllow,
                decisionLogEntity.requestedBy,
                decisionLogEntity.reqId,
                decisionLogEntity.opaInstanceId,
                decisionLogEntity.opaVersion,
                decisionLogEntity.service,
                decisionLogEntity.bundles,
                decisionLogEntity.raw,
                decisionLogEntity.createdAt
        );
    }

    private BooleanExpression cursorCondition(OffsetDateTime cursor) {
        return cursor != null ? decisionLogEntity.ts.lt(cursor) : null;
    }

    private BooleanExpression timestampFrom(OffsetDateTime from) {
        return from != null ? decisionLogEntity.ts.goe(from) : null;
    }

    private BooleanExpression timestampTo(OffsetDateTime to) {
        return to != null ? decisionLogEntity.ts.loe(to) : null;
    }

    private BooleanExpression allowEquals(Boolean allow) {
        return allow != null ? decisionLogEntity.overallAllow.eq(allow) : null;
    }

    private BooleanExpression serviceEquals(String service) {
        return StringUtils.hasText(service) ? decisionLogEntity.service.eq(service) : null;
    }

    private BooleanExpression pathContains(String path) {
        return StringUtils.hasText(path) ? decisionLogEntity.path.contains(path) : null;
    }
}
