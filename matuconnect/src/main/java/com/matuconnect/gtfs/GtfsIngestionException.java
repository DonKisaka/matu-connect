package com.matuconnect.gtfs;


public class GtfsIngestionException extends RuntimeException {

    public GtfsIngestionException(String message, Throwable cause) {
        super(message, cause);
    }
}