package com.example.opa.policydecisionlog.shared.api;

import com.example.opa.policydecisionlog.command.infra.kafka.exception.DecisionLogPublishException;
import com.example.opa.policydecisionlog.shared.exception.DecisionNotFoundException;
import com.example.opa.policydecisionlog.shared.exception.MissingDecisionIdException;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(DecisionNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleDecisionNotFound(
            DecisionNotFoundException ex,
            HttpServletRequest request
    ) {
        log.warn("Decision not found: {}", ex.getDecisionId());

        ErrorResponse response = ErrorResponse.of(
                HttpStatus.NOT_FOUND.value(),
                "Not Found",
                ex.getMessage(),
                request.getRequestURI()
        );

        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
    }

    @ExceptionHandler(DecisionLogPublishException.class)
    public ResponseEntity<ErrorResponse> handleDecisionLogPublishException(
            DecisionLogPublishException ex,
            HttpServletRequest request
    ) {
        log.error("Failed to publish decision log: {}", ex.getMessage(), ex);

        ErrorResponse response = ErrorResponse.of(
                HttpStatus.SERVICE_UNAVAILABLE.value(),
                "Service Unavailable",
                ex.getMessage(),
                request.getRequestURI()
        );

        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(response);
    }

    @ExceptionHandler(MissingDecisionIdException.class)
    public ResponseEntity<ErrorResponse> handleMissingDecisionIdException(
            MissingDecisionIdException ex,
            HttpServletRequest request
    ) {
        log.warn("Missing decisionId: {}", ex.getMessage());

        ErrorResponse response = ErrorResponse.of(
                HttpStatus.BAD_REQUEST.value(),
                "Bad Request",
                ex.getMessage(),
                request.getRequestURI()
        );

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }
}
