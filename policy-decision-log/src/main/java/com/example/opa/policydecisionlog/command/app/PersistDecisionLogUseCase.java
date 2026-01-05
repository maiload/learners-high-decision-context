package com.example.opa.policydecisionlog.command.app;

import com.example.opa.policydecisionlog.command.app.dto.DecisionLogIngestCommand;
import com.example.opa.policydecisionlog.command.app.dto.PersistResult;
import com.example.opa.policydecisionlog.command.app.error.DataErrorException;
import com.example.opa.policydecisionlog.command.app.error.ErrorHandler;
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
    private final ErrorHandler errorHandler;

    @Transactional
    public PersistResult execute(List<DecisionLogIngestCommand> commands) {
        log.debug("Persisting {} decision log(s) to database", commands.size());
        return saveWithRetry(commands);
    }

    private PersistResult saveWithRetry(List<DecisionLogIngestCommand> commands) {
        for (int attempt = 0; attempt < MAX_RETRIES; attempt++) {
            try {
                persistence.saveAll(commands);
                return PersistResult.SUCCESS;
            } catch (Exception e) {
                log.warn("DB save failed (attempt {}/{}): {}", attempt + 1, MAX_RETRIES, e.getMessage());
                if (attempt == MAX_RETRIES - 1) {
                    return handleError(commands, e);
                }
                sleep(BACKOFF_MS[attempt]);
            }
        }
        return PersistResult.FAILED;
    }

    private PersistResult handleError(List<DecisionLogIngestCommand> commands, Exception e) {
        log.error("DB save failed after {} retries, delegating to error handler", MAX_RETRIES, e);
        try {
            errorHandler.handle(commands, e);
            return PersistResult.PARKED;
        } catch (DataErrorException ex) {
            throw ex;
        } catch (Exception ex) {
            log.error("Error handler failed, cannot park commands", ex);
            return PersistResult.FAILED;
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
