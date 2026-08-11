package com.cbs.logistics.location_service.exception;

/** Package Service injoignable ou en erreur (503). */
public class PackageServiceUnavailableException extends RuntimeException {

    public PackageServiceUnavailableException(String message) {
        super(message);
    }
}
