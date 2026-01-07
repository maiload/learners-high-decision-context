package com.example.opa.policydecisionlog.shared.kafka;

public final class ParkingHeaders {

    private ParkingHeaders() {}

    public static final String RETRY_ATTEMPT = "x-retry-attempt";
    public static final String NOT_BEFORE = "x-not-before";
}
