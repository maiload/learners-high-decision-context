package com.example.opa.policydecisionlog.command.infra.file;

import com.example.opa.policydecisionlog.command.app.dto.InfrastructureFailureEvent;
import com.example.opa.policydecisionlog.command.app.port.InfrastructureFailureWriter;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import tools.jackson.databind.json.JsonMapper;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

@Slf4j
@Component
@RequiredArgsConstructor
public class InfrastructureFailureFileWriter implements InfrastructureFailureWriter {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    @Value("${opa.infra-failure.path:./logs/infra-failures}")
    private String path;

    private final JsonMapper jsonMapper;

    @PostConstruct
    void init() {
        try {
            Files.createDirectories(Path.of(path));
            log.info("Infrastructure failure log directory initialized: {}", path);
        } catch (IOException e) {
            log.error("Failed to create infrastructure failure log directory: {}", path, e);
        }
    }

    @Override
    public void write(ConsumerRecord<?, ?> record, Exception exception) {
        String fileName = String.format("infra-failure-%s.jsonl", LocalDate.now().format(DATE_FORMATTER));
        Path filePath = Path.of(path, fileName);

        try {
            InfrastructureFailureEvent event = InfrastructureFailureEvent.fromRecord(record, exception);
            String json = jsonMapper.writeValueAsString(event);
            Files.writeString(filePath, json + System.lineSeparator(),
                    StandardOpenOption.CREATE, StandardOpenOption.APPEND);
            log.info("Infrastructure failure written: topic={}, partition={}, offset={}",
                    record.topic(), record.partition(), record.offset());
        } catch (IOException e) {
            log.error("Failed to write infrastructure failure: topic={}, partition={}, offset={}",
                    record.topic(), record.partition(), record.offset(), e);
        }
    }
}
