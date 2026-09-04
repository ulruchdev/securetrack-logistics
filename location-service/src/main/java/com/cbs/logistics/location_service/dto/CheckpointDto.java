package com.cbs.logistics.location_service.dto;

public record CheckpointDto(
    Long id,
    Long siteId,
    String name,
    Boolean active
) {}
