package com.example.opa.policydecisionlog.query.api.mapper;

import com.example.opa.policydecisionlog.query.api.dto.DecisionLogSearchRequest;
import com.example.opa.policydecisionlog.query.app.dto.DecisionLogSearchQuery;
import org.springframework.stereotype.Component;

@Component
public class RequestToQueryMapper {

    private static final int DEFAULT_LIMIT = 20;
    private static final int MAX_LIMIT = 100;

    public DecisionLogSearchQuery toQuery(DecisionLogSearchRequest request) {
        int resolvedLimit = resolveLimit(request.limit());
        return new DecisionLogSearchQuery(
                request.from(),
                request.to(),
                request.allow(),
                request.service(),
                request.path(),
                resolvedLimit,
                request.cursor()
        );
    }

    private int resolveLimit(Integer limit) {
        if (limit == null || limit <= 0) {
            return DEFAULT_LIMIT;
        }
        return Math.min(limit, MAX_LIMIT);
    }
}
