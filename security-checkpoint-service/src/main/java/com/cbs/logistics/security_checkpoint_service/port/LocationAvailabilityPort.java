package com.cbs.logistics.security_checkpoint_service.port;

import java.io.Serializable;

/**
 * Port applicatif : expose UNIQUEMENT la vérification nécessaire auprès du
 * Location Service, sans exposer le type Feign. L'implémentation (adapter)
 * encapsule le client HTTP et gère la mise en cache (Redis).
 */
public interface LocationAvailabilityPort {

    /**
     * Vérifie la disponibilité d'un checkpoint.
     *
     * @param checkpointId identifiant du checkpoint dans le Location Service
     * @return informations de disponibilité du checkpoint
     * @throws com.cbs.logistics.security_checkpoint_service.exception.LocationNotFoundException
     *         si le checkpoint n'existe pas
     * @throws com.cbs.logistics.security_checkpoint_service.exception.LocationServiceUnavailableException
     *         si le Location Service est indisponible
     */
    CheckpointAvailability getCheckpointAvailability(Long checkpointId);

    /**
     * Résultat mis en cache Redis : doit être Serializable.
     */
    record CheckpointAvailability(boolean active, Long siteId) implements Serializable {}
}
