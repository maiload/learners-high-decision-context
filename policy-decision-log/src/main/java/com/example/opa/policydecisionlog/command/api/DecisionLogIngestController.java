package com.example.opa.policydecisionlog.command.api;

import com.example.opa.policydecisionlog.command.api.dto.DecisionLogIngestRequest;
import com.example.opa.policydecisionlog.command.api.mapper.RequestToCommandMapper;
import com.example.opa.policydecisionlog.command.app.DecisionLogCommandService;
import com.example.opa.policydecisionlog.command.app.dto.DecisionLogIngestCommand;
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
    private final RequestToCommandMapper mapper;

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
