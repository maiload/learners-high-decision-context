package com.example.opa.policydecisionlog.query.api;

import com.example.opa.policydecisionlog.query.api.dto.DecisionLogCursorResponse;
import com.example.opa.policydecisionlog.query.api.dto.DecisionLogResponse;
import com.example.opa.policydecisionlog.query.api.dto.DecisionLogSearchRequest;
import com.example.opa.policydecisionlog.query.api.mapper.RequestToQueryMapper;
import com.example.opa.policydecisionlog.query.app.DecisionLogQueryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/decisions")
@RequiredArgsConstructor
public class DecisionLogQueryController {

    private final DecisionLogQueryService queryService;
    private final RequestToQueryMapper mapper;

    @GetMapping("/{decisionId}")
    public DecisionLogResponse getDecision(@PathVariable UUID decisionId) {
        log.info("GET /decisions/{}", decisionId);
        return DecisionLogResponse.from(queryService.getByDecisionId(decisionId));
    }

    @GetMapping
    public DecisionLogCursorResponse searchDecisions(DecisionLogSearchRequest request) {
        log.info("GET /decisions with params: {}", request);
        var query = mapper.toQuery(request);
        return DecisionLogCursorResponse.from(queryService.search(query), query.limit());
    }
}
