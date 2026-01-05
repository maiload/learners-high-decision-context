package com.example.opa.policydecisionlog.command.app.error;

import lombok.Getter;

@Getter
public class DataErrorException extends RuntimeException {

    private final int failedIndex;

    public DataErrorException(int failedIndex, Throwable cause) {
        super("Data error at index " + failedIndex, cause);
        this.failedIndex = failedIndex;
    }
}
