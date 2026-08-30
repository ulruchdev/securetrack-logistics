package com.cbs.logistics.location_service.dto;

public record SiteDto(
    Long id,
    String name,
    String address,
    Double latitude,
    Double longitude,
    Boolean active
) {}
