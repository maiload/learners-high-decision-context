package com.example.opa.policydecisionlog.command.app;

import com.example.opa.policydecisionlog.command.app.dto.DecisionLogIngestCommand;
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

    @Transactional
    public void ingestLogs(List<DecisionLogIngestCommand> commands) {
        log.debug("Saving {} decision log(s)", commands.size());
        repository.saveAll(commands);
    }
}
