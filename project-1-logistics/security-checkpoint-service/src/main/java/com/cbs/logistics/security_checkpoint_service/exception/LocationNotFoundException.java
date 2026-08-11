package com.cbs.logistics.security_checkpoint_service.exception;

/** Localisation introuvable auprès du Location Service (404). */
public class LocationNotFoundException extends RuntimeException {

    public LocationNotFoundException(String message) {
        super(message);
    }
}
