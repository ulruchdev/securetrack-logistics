package com.cbs.logistics.security_checkpoint_service.exception;

public class CheckpointLogNotFoundException extends RuntimeException {
    public CheckpointLogNotFoundException(Long id) {
        super("Checkpoint log not found with id: " + id);
    }
}