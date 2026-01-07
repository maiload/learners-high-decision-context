package com.example.opa.policydecisionlog.command.app.error;

import com.example.opa.policydecisionlog.command.app.dto.DecisionLogIngestCommand;
import com.example.opa.policydecisionlog.command.app.port.DecisionLogPersistence;
import com.example.opa.policydecisionlog.command.app.port.ParkingLotPublisher;
import com.example.opa.policydecisionlog.shared.metrics.DecisionLogMetrics;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class ErrorHandler {

    private final ErrorClassifier errorClassifier;
    private final ParkingLotPublisher parkingLotPublisher;
    private final DecisionLogPersistence persistence;
    private final DecisionLogMetrics metrics;

    public void handleRecovery(DecisionLogIngestCommand command, int attempt, Exception error) {
        if (errorClassifier.isRetryable(error)) {
            log.info("Retryable error in recovery, retrying with attempt {}: decisionId={}",
                    attempt + 1, command.decisionId());
            parkingLotPublisher.retry(command, attempt + 1);
            metrics.recordParkingSent(1);
        } else {
            log.info("Non-retryable error in recovery, sending to DLQ: decisionId={}",
                    command.decisionId());
            parkingLotPublisher.toDlq(command);
            metrics.recordDlqSent(1);
        }
    }

    public void handle(List<DecisionLogIngestCommand> commands, Throwable error) {
        if (errorClassifier.isRetryable(error)) {
            log.info("Retryable error detected, sending {} command(s) to parking lot", commands.size());
            parkingLotPublisher.publish(commands);
            metrics.recordParkingSent(commands.size());
        } else {
            log.info("Non-retryable error detected, starting bisect for {} command(s)", commands.size());
            List<DecisionLogIngestCommand> failedCommands = new ArrayList<>();
            bisectAndCollect(commands, failedCommands);

            for (DecisionLogIngestCommand failedCommand : failedCommands) {
                parkingLotPublisher.toDlq(failedCommand);
                log.warn("Sent failed record to DLQ: decisionId={}", failedCommand.decisionId());
            }

            if (!failedCommands.isEmpty()) {
                metrics.recordDlqSent(failedCommands.size());
                log.info("Bisect completed: {} record(s) sent to DLQ", failedCommands.size());
            }
        }
    }

    private void bisectAndCollect(List<DecisionLogIngestCommand> commands,
                                  List<DecisionLogIngestCommand> failedCommands) {
        if (commands.isEmpty()) {
            return;
        }

        try {
            persistence.saveAll(commands);
            log.debug("Bisect batch saved successfully: {} command(s)", commands.size());
        } catch (Exception e) {
            if (commands.size() == 1) {
                DecisionLogIngestCommand failedCommand = commands.getFirst();
                log.warn("Single record failed: decisionId={}", failedCommand.decisionId());
                failedCommands.add(failedCommand);
                return;
            }

            int mid = commands.size() / 2;
            List<DecisionLogIngestCommand> left = commands.subList(0, mid);
            List<DecisionLogIngestCommand> right = commands.subList(mid, commands.size());

            log.debug("Bisect split: {} -> {} + {}", commands.size(), left.size(), right.size());

            bisectAndCollect(left, failedCommands);
            bisectAndCollect(right, failedCommands);
        }
    }
}
