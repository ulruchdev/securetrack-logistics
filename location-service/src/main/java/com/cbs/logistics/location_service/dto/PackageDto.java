package com.cbs.logistics.location_service.dto;

public record PackageDto(
        Long packageId,
        String description,
        String packageName,
        String packageType,
        Double weight,
        boolean fragile,
        String packageStatus
) {
}