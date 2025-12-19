package com.example.opa.policydecisionlog.command.app;

import com.example.opa.policydecisionlog.command.app.model.IngestDecisionLogCommand;
import com.example.opa.policydecisionlog.command.infra.DecisionLogCommandRepository;
import com.example.opa.policydecisionlog.command.infra.model.DecisionLogRow;
import com.example.opa.policydecisionlog.shared.mapper.CommandToRowMapper;
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
    private final CommandToRowMapper commandToRowMapper;

    @Transactional
    public void ingestLogs(List<IngestDecisionLogCommand> commands) {
        log.debug("Mapping {} command(s) to entity rows", commands.size());

        List<DecisionLogRow> rows = commands.stream()
                .map(commandToRowMapper::toDecisionLogRow)
                .toList();

        log.debug("Saving {} decision log row(s) to database", rows.size());
        repository.saveAll(rows);
    }
}
