package com.example.opa.policydecisionlog.query.infra;

import com.example.opa.policydecisionlog.query.app.dto.DecisionLogSearchQuery;
import com.example.opa.policydecisionlog.query.infra.model.DecisionLogRow;
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

import static com.example.opa.policydecisionlog.command.infra.model.QDecisionLog.decisionLog;

@Repository
@RequiredArgsConstructor
public class DecisionLogQueryRepositoryImpl implements DecisionLogQueryRepository {

    private final JPAQueryFactory queryFactory;

    @Override
    public Optional<DecisionLogRow> findByDecisionId(UUID decisionId) {
        DecisionLogRow result = queryFactory
                .select(projection())
                .from(decisionLog)
                .where(decisionLog.decisionId.eq(decisionId))
                .fetchOne();

        return Optional.ofNullable(result);
    }

    @Override
    public List<DecisionLogRow> search(DecisionLogSearchQuery query) {
        return queryFactory
                .select(projection())
                .from(decisionLog)
                .where(
                        cursorCondition(query.cursor()),
                        timestampFrom(query.from()),
                        timestampTo(query.to()),
                        allowEquals(query.allow()),
                        serviceEquals(query.service()),
                        pathContains(query.path())
                )
                .orderBy(decisionLog.ts.desc())
                .limit(query.limit() + 1L)
                .fetch();
    }

    private com.querydsl.core.types.ConstructorExpression<DecisionLogRow> projection() {
        return Projections.constructor(
                DecisionLogRow.class,
                decisionLog.id,
                decisionLog.decisionId,
                decisionLog.ts,
                decisionLog.path,
                decisionLog.overallAllow,
                decisionLog.requestedBy,
                decisionLog.reqId,
                decisionLog.opaInstanceId,
                decisionLog.opaVersion,
                decisionLog.service,
                decisionLog.bundles,
                decisionLog.raw,
                decisionLog.createdAt
        );
    }

    private BooleanExpression cursorCondition(OffsetDateTime cursor) {
        return cursor != null ? decisionLog.ts.lt(cursor) : null;
    }

    private BooleanExpression timestampFrom(OffsetDateTime from) {
        return from != null ? decisionLog.ts.goe(from) : null;
    }

    private BooleanExpression timestampTo(OffsetDateTime to) {
        return to != null ? decisionLog.ts.loe(to) : null;
    }

    private BooleanExpression allowEquals(Boolean allow) {
        return allow != null ? decisionLog.overallAllow.eq(allow) : null;
    }

    private BooleanExpression serviceEquals(String service) {
        return StringUtils.hasText(service) ? decisionLog.service.eq(service) : null;
    }

    private BooleanExpression pathContains(String path) {
        return StringUtils.hasText(path) ? decisionLog.path.contains(path) : null;
    }
}
