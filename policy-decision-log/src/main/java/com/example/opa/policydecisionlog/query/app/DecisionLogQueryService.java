package com.example.opa.policydecisionlog.query.app;

import com.example.opa.policydecisionlog.query.app.dto.CursorPage;
import com.example.opa.policydecisionlog.query.app.dto.DecisionContext;
import com.example.opa.policydecisionlog.query.app.dto.DecisionContextSummary;
import com.example.opa.policydecisionlog.query.app.dto.DecisionLogReadModel;
import com.example.opa.policydecisionlog.query.app.dto.DecisionLogSearchQuery;
import com.example.opa.policydecisionlog.shared.exception.DecisionNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.function.Function;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DecisionLogQueryService {

    private final DecisionLogQueryRepository repository;
    private final DecisionContextAssembler contextAssembler;

    public DecisionLogReadModel getByDecisionId(UUID decisionId) {
        log.debug("Finding decision log by decisionId: {}", decisionId);
        return repository.findByDecisionId(decisionId)
                .orElseThrow(() -> new DecisionNotFoundException(decisionId));
    }

    public DecisionContext getContextByDecisionId(UUID decisionId) {
        var readModel = getByDecisionId(decisionId);
        return contextAssembler.assemble(readModel);
    }

    public CursorPage<DecisionLogReadModel> search(DecisionLogSearchQuery query) {
        log.debug("Searching decision logs: {}", query);
        return searchAndPage(query, readModel -> readModel);
    }

    public CursorPage<DecisionContextSummary> searchContextSummaries(DecisionLogSearchQuery query) {
        log.debug("Searching context summaries: {}", query);
        return searchAndPage(query, contextAssembler::assembleSummary);
    }

    private <T> CursorPage<T> searchAndPage(
            DecisionLogSearchQuery query,
            Function<DecisionLogReadModel, T> mapper) {
        List<DecisionLogReadModel> readModels = repository.search(query);

        boolean hasNext = readModels.size() > query.limit();
        List<DecisionLogReadModel> content = hasNext ? readModels.subList(0, query.limit()) : readModels;

        List<T> mapped = content.stream().map(mapper).toList();
        var nextCursor = hasNext ? content.getLast().timestamp() : null;

        return new CursorPage<>(mapped, nextCursor);
    }
}
