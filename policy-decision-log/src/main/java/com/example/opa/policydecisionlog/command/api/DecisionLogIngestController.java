package com.example.opa.policydecisionlog.command.api;

import com.example.opa.policydecisionlog.command.infra.kafka.DecisionLogProducer;
import tools.jackson.databind.json.JsonMapper;
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
import java.util.Map;

@Slf4j
@RestController
@RequiredArgsConstructor
@Tag(name = "Decision Log Ingest")
public class DecisionLogIngestController {

    private final DecisionLogProducer producer;
    private final JsonMapper jsonMapper;

    @Operation(summary = "Decision Log 수집", description = "OPA에서 전송한 Decision Log를 Kafka로 발행합니다.")
    @ApiResponse(responseCode = "204", description = "No Content")
    @ApiResponse(responseCode = "400", description = "Bad Request")
    @PostMapping("/logs")
    public ResponseEntity<Void> ingestLogs(@RequestBody List<Map<String, Object>> requests) {
        log.info("Received {} decision log(s), publishing to Kafka", requests.size());

        String payload = jsonMapper.writeValueAsString(requests);
        producer.send(payload);

        return ResponseEntity.noContent().build();
    }
}
