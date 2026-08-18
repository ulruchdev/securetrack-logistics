package com.cbs.logistics.security_checkpoint_service.client;

import com.cbs.logistics.security_checkpoint_service.port.LocationAvailabilityPort;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Component;

/**
 * Adapter Feign : implémente le port applicatif en encapsulant le client
 * LocationServiceClient. Le résultat est mis en cache (Redis) pour éviter
 * d'appeler le Location Service à chaque création de checkpoint.
 */
@Component
@RequiredArgsConstructor
public class LocationAvailabilityAdapter implements LocationAvailabilityPort {

    private final LocationServiceClient locationServiceClient;

    @Override
    @Cacheable(cacheNames = "locationAvailability", key = "#locationId")
    public LocationAvailability getLocation(String locationId) {
        LocationServiceClient.LocationDto dto = locationServiceClient.getLocationById(locationId);
        return new LocationAvailability(dto.packageId(), dto.checkpointAvailable());
    }
}
