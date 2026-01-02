package com.example.opa.policydecisionlog.command.app.port;

import com.example.opa.policydecisionlog.command.app.dto.DecisionLogIngestCommand;

import java.util.List;

public interface DecisionLogPersistence {

    void saveAll(List<DecisionLogIngestCommand> commands);
}
