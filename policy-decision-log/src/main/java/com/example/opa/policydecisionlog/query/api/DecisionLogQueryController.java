package com.example.opa.policydecisionlog.query.api;

import com.example.opa.policydecisionlog.query.api.dto.DecisionLogCursorResponse;
import com.example.opa.policydecisionlog.query.api.dto.DecisionLogResponse;
import com.example.opa.policydecisionlog.query.api.dto.DecisionLogSearchRequest;
import com.example.opa.policydecisionlog.query.api.mapper.RequestToQueryMapper;
import com.example.opa.policydecisionlog.query.app.DecisionLogQueryService;
import com.example.opa.policydecisionlog.shared.api.ErrorResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
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
@Tag(name = "Decision Log Query")
public class DecisionLogQueryController {

    private final DecisionLogQueryService queryService;
    private final RequestToQueryMapper mapper;

    @Operation(summary = "Decision Log 단건 조회", description = "Decision ID로 단건 Decision Log를 조회합니다.")
    @ApiResponse(responseCode = "200", description = "OK")
    @ApiResponse(responseCode = "404", description = "Not Found",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @GetMapping("/{decisionId}")
    public DecisionLogResponse getDecision(
            @Parameter(description = "Decision ID", required = true)
            @PathVariable UUID decisionId) {
        log.info("GET /decisions/{}", decisionId);
        return DecisionLogResponse.from(queryService.getByDecisionId(decisionId));
    }

    @Operation(summary = "Decision Log 목록 조회", description = "필터 조건으로 Decision Log 목록을 조회합니다. (Cursor 기반 페이징을 지원)")
    @ApiResponse(responseCode = "200", description = "OK")
    @GetMapping
    public DecisionLogCursorResponse searchDecisions(DecisionLogSearchRequest request) {
        log.info("GET /decisions with params: {}", request);
        var query = mapper.toQuery(request);
        return DecisionLogCursorResponse.from(queryService.search(query), query.limit());
    }
}
