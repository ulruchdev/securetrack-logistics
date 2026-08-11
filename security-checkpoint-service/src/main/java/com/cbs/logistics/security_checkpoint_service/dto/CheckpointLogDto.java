package com.cbs.logistics.security_checkpoint_service.dto;

import com.cbs.logistics.security_checkpoint_service.entity.CheckpointResult;

import java.time.LocalDateTime;

public record CheckpointLogDto(
        Long id,
        Long packageId,
        String locationId,
        LocalDateTime checkpointTime,
        CheckpointResult result,
        String comment,
        String createdBy
) {
}