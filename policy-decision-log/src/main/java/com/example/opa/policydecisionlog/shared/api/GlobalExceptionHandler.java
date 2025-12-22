package com.example.opa.policydecisionlog.shared.api;

import com.example.opa.policydecisionlog.shared.exception.DecisionNotFoundException;
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
}
