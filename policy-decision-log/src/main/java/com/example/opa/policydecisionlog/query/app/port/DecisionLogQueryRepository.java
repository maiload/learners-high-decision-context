package com.example.opa.policydecisionlog.query.app.port;

import com.example.opa.policydecisionlog.query.app.dto.DecisionLogReadModel;
import com.example.opa.policydecisionlog.query.app.dto.DecisionLogSearchQuery;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface DecisionLogQueryRepository {

    Optional<DecisionLogReadModel> findByDecisionId(UUID decisionId);

    List<DecisionLogReadModel> search(DecisionLogSearchQuery query);
}
