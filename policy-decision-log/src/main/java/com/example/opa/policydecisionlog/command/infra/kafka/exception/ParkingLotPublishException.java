package com.example.opa.policydecisionlog.command.infra.kafka.exception;

public class ParkingLotPublishException extends RuntimeException {

    public ParkingLotPublishException(String topic, Throwable cause) {
        super("Failed to publish to Parking Lot: " + topic, cause);
    }
}
