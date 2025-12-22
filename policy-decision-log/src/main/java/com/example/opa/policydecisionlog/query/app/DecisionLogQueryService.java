package com.example.opa.policydecisionlog.query.app;

import com.example.opa.policydecisionlog.query.infra.model.DecisionLogRow;
import com.example.opa.policydecisionlog.query.app.dto.DecisionLogSearchQuery;
import com.example.opa.policydecisionlog.query.infra.DecisionLogQueryRepository;
import com.example.opa.policydecisionlog.shared.exception.DecisionNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DecisionLogQueryService {

    private final DecisionLogQueryRepository repository;

    public DecisionLogRow getByDecisionId(UUID decisionId) {
        log.debug("Finding decision log by decisionId: {}", decisionId);
        return repository.findByDecisionId(decisionId)
                .orElseThrow(() -> new DecisionNotFoundException(decisionId));
    }

    public List<DecisionLogRow> search(DecisionLogSearchQuery query) {
        log.debug("Searching decision logs: {}", query);
        return repository.search(query);
    }
}
