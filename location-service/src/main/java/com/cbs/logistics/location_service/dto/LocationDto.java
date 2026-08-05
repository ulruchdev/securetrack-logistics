package com.cbs.logistics.location_service.dto;

public record LocationDto( String locationId,Long packageId, String city, String zone, Boolean checkpointAvailable) {
}
