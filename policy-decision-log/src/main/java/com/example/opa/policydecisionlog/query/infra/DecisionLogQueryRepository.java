package com.example.opa.policydecisionlog.query.infra;

import com.example.opa.policydecisionlog.query.app.dto.DecisionLogSearchQuery;
import com.example.opa.policydecisionlog.query.infra.model.DecisionLogRow;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface DecisionLogQueryRepository {

    Optional<DecisionLogRow> findByDecisionId(UUID decisionId);

    List<DecisionLogRow> search(DecisionLogSearchQuery query);
}
