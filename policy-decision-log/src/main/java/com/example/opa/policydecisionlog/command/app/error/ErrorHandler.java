package com.example.opa.policydecisionlog.command.app.error;

import com.example.opa.policydecisionlog.command.app.dto.DecisionLogIngestCommand;
import com.example.opa.policydecisionlog.command.app.port.DecisionLogPersistence;
import com.example.opa.policydecisionlog.command.app.port.ParkingLotPublisher;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class ErrorHandler {

    private final ErrorClassifier errorClassifier;
    private final ParkingLotPublisher parkingLotPublisher;
    private final DecisionLogPersistence persistence;

    public void handle(List<DecisionLogIngestCommand> commands, Throwable error) {
        if (errorClassifier.isRetryable(error)) {
            log.info("Retryable error detected, sending {} command(s) to parking lot", commands.size());
            parkingLotPublisher.publish(commands);
        } else {
            log.info("Non-retryable error detected, starting bisect for {} command(s)", commands.size());
            bisectAndThrow(commands, commands, error);
        }
    }

    private void bisectAndThrow(List<DecisionLogIngestCommand> commands,
                                List<DecisionLogIngestCommand> originalCommands,
                                Throwable error) {
        if (commands.isEmpty()) {
            return;
        }

        if (commands.size() == 1) {
            DecisionLogIngestCommand failedCommand = commands.getFirst();
            int failedIndex = originalCommands.indexOf(failedCommand);
            log.warn("Single record failed at index {}: decisionId={}", failedIndex, failedCommand.decisionId());
            throw new DataErrorException(failedIndex, error);
        }

        try {
            persistence.saveAll(commands);
            log.debug("Bisect batch saved successfully: {} command(s)", commands.size());
        } catch (Exception e) {
            int mid = commands.size() / 2;
            List<DecisionLogIngestCommand> left = commands.subList(0, mid);
            List<DecisionLogIngestCommand> right = commands.subList(mid, commands.size());

            log.debug("Bisect split: {} -> {} + {}", commands.size(), left.size(), right.size());

            bisectAndThrow(left, originalCommands, e);
            bisectAndThrow(right, originalCommands, e);
        }
    }
}
