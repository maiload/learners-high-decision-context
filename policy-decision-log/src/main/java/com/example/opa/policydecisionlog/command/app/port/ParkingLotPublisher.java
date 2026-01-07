package com.example.opa.policydecisionlog.command.app.port;

import com.example.opa.policydecisionlog.command.app.dto.DecisionLogIngestCommand;

import java.util.List;

public interface ParkingLotPublisher {

    void publish(List<DecisionLogIngestCommand> commands);

    void retry(DecisionLogIngestCommand command, int attempt);

    void toDlq(DecisionLogIngestCommand command);

    void toParkingDlq(DecisionLogIngestCommand command);
}
