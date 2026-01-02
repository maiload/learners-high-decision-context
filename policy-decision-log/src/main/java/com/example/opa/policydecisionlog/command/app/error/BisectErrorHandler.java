package com.example.opa.policydecisionlog.command.app.error;

import com.example.opa.policydecisionlog.command.app.dto.DecisionLogIngestCommand;
import com.example.opa.policydecisionlog.command.app.port.DecisionLogPersistence;
import com.example.opa.policydecisionlog.command.app.port.DlqPublisher;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class BisectErrorHandler {

    private final DecisionLogPersistence persistence;
    private final DlqPublisher dlqPublisher;

    public void handleWithBisect(List<DecisionLogIngestCommand> commands) {
        log.warn("Starting bisect error handling for {} command(s)", commands.size());

        List<DecisionLogIngestCommand> failed = new ArrayList<>();
        bisect(commands, failed);

        if (!failed.isEmpty()) {
            log.info("Bisect complete: {} failed command(s) sent to DLQ", failed.size());
        }
    }

    private void bisect(List<DecisionLogIngestCommand> commands, List<DecisionLogIngestCommand> failed) {
        if (commands.isEmpty()) {
            return;
        }

        if (commands.size() == 1) {
            DecisionLogIngestCommand command = commands.getFirst();
            log.warn("Single record failed, sending to DLQ: decisionId={}", command.decisionId());
            dlqPublisher.publish(command);
            failed.add(command);
            return;
        }

        try {
            persistence.saveAll(commands);
            log.debug("Bisect batch saved successfully: {} command(s)", commands.size());
        } catch (Exception e) {
            int mid = commands.size() / 2;
            List<DecisionLogIngestCommand> left = commands.subList(0, mid);
            List<DecisionLogIngestCommand> right = commands.subList(mid, commands.size());

            log.debug("Bisect split: {} -> {} + {}", commands.size(), left.size(), right.size());

            bisect(left, failed);
            bisect(right, failed);
        }
    }
}
