package com.cbs.logistics.location_service.exception;

/** Colis introuvable auprès du Package Service (404). */
public class PackageNotFoundException extends RuntimeException {

    public PackageNotFoundException(String message) {
        super(message);
    }
}
