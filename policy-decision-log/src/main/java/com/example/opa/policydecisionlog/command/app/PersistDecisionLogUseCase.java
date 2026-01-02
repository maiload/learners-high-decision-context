package com.example.opa.policydecisionlog.command.app;

import com.example.opa.policydecisionlog.command.app.dto.DecisionLogIngestCommand;
import com.example.opa.policydecisionlog.command.app.error.AsyncErrorHandler;
import com.example.opa.policydecisionlog.command.app.port.DecisionLogPersistence;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class PersistDecisionLogUseCase {

    private static final int MAX_RETRIES = 2;
    private static final long[] BACKOFF_MS = {1000, 2000};

    private final DecisionLogPersistence persistence;
    private final AsyncErrorHandler asyncErrorHandler;

    @Transactional
    public void execute(List<DecisionLogIngestCommand> commands) {
        log.debug("Persisting {} decision log(s) to database", commands.size());
        saveWithRetry(commands);
    }

    private void saveWithRetry(List<DecisionLogIngestCommand> commands) {
        for (int attempt = 0; attempt < MAX_RETRIES; attempt++) {
            try {
                persistence.saveAll(commands);
                return;
            } catch (Exception e) {
                log.warn("DB save failed (attempt {}/{}): {}", attempt + 1, MAX_RETRIES, e.getMessage());
                if (attempt < MAX_RETRIES - 1) {
                    sleep(BACKOFF_MS[attempt]);
                } else {
                    log.error("DB save failed after {} retries, delegating to error handler", MAX_RETRIES, e);
                    asyncErrorHandler.handleAsync(commands, e);
                    return;
                }
            }
        }
    }

    private void sleep(long ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
