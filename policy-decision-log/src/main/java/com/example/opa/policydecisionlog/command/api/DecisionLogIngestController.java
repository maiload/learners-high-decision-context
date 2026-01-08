package com.example.opa.policydecisionlog.command.api;

import com.example.opa.policydecisionlog.command.app.usecase.PublishDecisionLogUseCase;
import com.example.opa.policydecisionlog.shared.metrics.DecisionLogMetrics;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequiredArgsConstructor
@Tag(name = "Decision Log Ingest")
public class DecisionLogIngestController {

    private final PublishDecisionLogUseCase publishDecisionLogUseCase;
    private final DecisionLogMetrics metrics;

    @Operation(summary = "Decision Log 수집", description = "OPA에서 전송한 Decision Log를 Kafka로 발행합니다.")
    @ApiResponse(responseCode = "204", description = "No Content")
    @ApiResponse(responseCode = "400", description = "Bad Request")
    @ApiResponse(responseCode = "503", description = "Service Unavailable")
    @PostMapping("/logs")
    public ResponseEntity<Void> ingestLogs(@RequestBody List<Map<String, Object>> requests) {
        publishDecisionLogUseCase.execute(requests);
        metrics.recordIngest(requests.size());
        return ResponseEntity.noContent().build();
    }
}
