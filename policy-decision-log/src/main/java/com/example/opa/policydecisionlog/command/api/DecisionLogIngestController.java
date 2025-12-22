package com.example.opa.policydecisionlog.command.api;

import com.example.opa.policydecisionlog.command.api.dto.DecisionLogIngestRequest;
import com.example.opa.policydecisionlog.command.api.mapper.RequestToCommandMapper;
import com.example.opa.policydecisionlog.command.app.DecisionLogCommandService;
import com.example.opa.policydecisionlog.command.app.dto.DecisionLogIngestCommand;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Slf4j
@RestController
@RequiredArgsConstructor
@Tag(name = "Decision Log Ingest")
public class DecisionLogIngestController {

    private final DecisionLogCommandService commandService;
    private final RequestToCommandMapper mapper;

    @Operation(summary = "Decision Log 수집", description = "OPA에서 전송한 Decision Log를 수집합니다.")
    @ApiResponse(responseCode = "204", description = "No Content")
    @ApiResponse(responseCode = "400", description = "Bad Request")
    @PostMapping("/logs")
    public ResponseEntity<Void> ingestLogs(@RequestBody List<DecisionLogIngestRequest> requests) {
        log.info("Received {} decision log(s) for ingestion", requests.size());

        List<DecisionLogIngestCommand> commands = requests.stream()
                .map(mapper::toCommand)
                .toList();
        commandService.ingestLogs(commands);

        log.info("Successfully ingested {} decision log(s)", commands.size());
        return ResponseEntity.noContent().build();
    }
}
