package com.example.opa.policydecisionlog.query.infra.mapper;

import com.example.opa.policydecisionlog.query.app.dto.DecisionLogReadModel;
import com.example.opa.policydecisionlog.query.infra.model.DecisionLogRow;
import org.springframework.stereotype.Component;

@Component
public class RowToReadModelMapper {

    public DecisionLogReadModel toReadModel(DecisionLogRow row) {
        return new DecisionLogReadModel(
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
