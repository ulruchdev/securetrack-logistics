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
    @Cacheable(cacheNames = "checkpointAvailability", key = "#checkpointId")
    public CheckpointAvailability getCheckpointAvailability(Long checkpointId) {
        LocationServiceClient.CheckpointDto dto = locationServiceClient.getCheckpointById(checkpointId);
        return new CheckpointAvailability(dto.active(), dto.siteId());
    }
}
