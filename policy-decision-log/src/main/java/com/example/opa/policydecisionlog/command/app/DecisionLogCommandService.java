package com.example.opa.policydecisionlog.command.app;

import com.example.opa.policydecisionlog.command.app.dto.DecisionLogIngestCommand;
import com.example.opa.policydecisionlog.command.app.mapper.CommandToEntityMapper;
import com.example.opa.policydecisionlog.command.infra.DecisionLogCommandRepository;
import com.example.opa.policydecisionlog.command.infra.model.DecisionLog;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DecisionLogCommandService {

    private final DecisionLogCommandRepository repository;
    private final CommandToEntityMapper mapper;

    @Transactional
    public void ingestLogs(List<DecisionLogIngestCommand> commands) {
        log.debug("Mapping {} command(s) to entities", commands.size());

        List<DecisionLog> entities = commands.stream()
                .map(mapper::toEntity)
                .toList();

        log.debug("Saving {} decision log(s) to database", entities.size());
        repository.saveAll(entities);
    }
}
