package com.cbs.logistics.location_service.dto;

public record EnrichedLocationDto(LocationDto location, PackageDto packageInfo) {
}