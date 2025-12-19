package com.example.opa.policydecisionlog.command.api;

import com.example.opa.policydecisionlog.command.api.dto.DecisionLogIngestRequest;
import com.example.opa.policydecisionlog.command.app.DecisionLogCommandService;
import com.example.opa.policydecisionlog.command.app.model.IngestDecisionLogCommand;
import com.example.opa.policydecisionlog.shared.mapper.ApiToCommandMapper;
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
public class DecisionLogIngestController {

    private final DecisionLogCommandService commandService;
    private final ApiToCommandMapper apiToCommandMapper;

    @PostMapping("/logs")
    public ResponseEntity<Void> ingestLogs(@RequestBody List<DecisionLogIngestRequest> requests) {
        log.info("Received {} decision log(s) for ingestion", requests.size());

        List<IngestDecisionLogCommand> commands = requests.stream()
                .map(apiToCommandMapper::toIngestDecisionLogCommand)
                .toList();
        commandService.ingestLogs(commands);

        log.info("Successfully ingested {} decision log(s)", commands.size());
        return ResponseEntity.noContent().build();
    }
}
