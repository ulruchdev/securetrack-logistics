package com.cbs.logistics.tracking_service.exception;

/** Ressource introuvable côté lecture -> traduite en HTTP 404. */
public class NotFoundException extends RuntimeException {

    public NotFoundException(String message) {
        super(message);
    }
}
