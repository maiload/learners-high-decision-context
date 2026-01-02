package com.example.opa.policydecisionlog.command.app.error;

import com.example.opa.policydecisionlog.command.app.dto.DecisionLogIngestCommand;
import com.example.opa.policydecisionlog.command.app.port.ParkingLotPublisher;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.concurrent.Executor;

@Slf4j
@Component
public class AsyncErrorHandler {

    private final Executor errorExecutor;
    private final ErrorClassifier errorClassifier;
    private final ParkingLotPublisher parkingLotPublisher;
    private final BisectErrorHandler bisectErrorHandler;

    public AsyncErrorHandler(
            @Qualifier("errorHandlerExecutor") Executor errorExecutor,
            ErrorClassifier errorClassifier,
            ParkingLotPublisher parkingLotPublisher,
            BisectErrorHandler bisectErrorHandler
    ) {
        this.errorExecutor = errorExecutor;
        this.errorClassifier = errorClassifier;
        this.parkingLotPublisher = parkingLotPublisher;
        this.bisectErrorHandler = bisectErrorHandler;
    }

    public void handleAsync(List<DecisionLogIngestCommand> commands, Throwable error) {
        errorExecutor.execute(() -> {
            try {
                handle(commands, error);
            } catch (Exception e) {
                log.error("Error handler failed", e);
            }
        });
    }

    private void handle(List<DecisionLogIngestCommand> commands, Throwable error) {
        if (errorClassifier.isRetryable(error)) {
            log.info("Retryable error detected, sending {} command(s) to parking lot", commands.size());
            parkingLotPublisher.publish(commands);
        } else {
            log.info("Non-retryable error detected, starting bisect for {} command(s)", commands.size());
            bisectErrorHandler.handleWithBisect(commands);
        }
    }
}
