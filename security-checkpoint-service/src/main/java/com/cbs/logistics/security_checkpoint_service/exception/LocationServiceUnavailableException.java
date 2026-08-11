package com.cbs.logistics.security_checkpoint_service.exception;

/** Location Service injoignable ou en erreur (503). */
public class LocationServiceUnavailableException extends RuntimeException {

    public LocationServiceUnavailableException(String message) {
        super(message);
    }
}
