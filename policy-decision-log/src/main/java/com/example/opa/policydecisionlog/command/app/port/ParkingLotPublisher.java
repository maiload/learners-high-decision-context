package com.example.opa.policydecisionlog.command.app.port;

import com.example.opa.policydecisionlog.command.app.dto.DecisionLogIngestCommand;

import java.util.List;

public interface ParkingLotPublisher {

    void publish(List<DecisionLogIngestCommand> commands);
}
