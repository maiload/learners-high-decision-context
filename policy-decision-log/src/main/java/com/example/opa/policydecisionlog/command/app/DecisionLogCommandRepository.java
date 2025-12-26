package com.example.opa.policydecisionlog.command.app;

import com.example.opa.policydecisionlog.command.app.dto.DecisionLogIngestCommand;

import java.util.List;

public interface DecisionLogCommandRepository {

    void saveAll(List<DecisionLogIngestCommand> commands);
}
