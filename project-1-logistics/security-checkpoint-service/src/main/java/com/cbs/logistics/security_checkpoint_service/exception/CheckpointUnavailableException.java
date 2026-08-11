package com.cbs.logistics.security_checkpoint_service.exception;

/** La localisation existe mais n'autorise pas les checkpoints (422 Unprocessable Entity). */
public class CheckpointUnavailableException extends RuntimeException {

    public CheckpointUnavailableException(String message) {
        super(message);
    }
}
