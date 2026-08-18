package com.cbs.logistics.security_checkpoint_service.port;

import java.io.Serializable;

/**
 * Port applicatif : expose UNIQUEMENT la vérification nécessaire auprès du
 * Location Service, sans exposer le type Feign. L'implémentation (adapter)
 * encapsule le client HTTP et gère la mise en cache (Redis).
 */
public interface LocationAvailabilityPort {

    /**
     * Récupère la disponibilité d'une localisation pour les checkpoints.
     *
     * @param locationId identifiant de la localisation
     * @return informations de la localisation (colis rattaché + disponibilité)
     * @throws com.cbs.logistics.security_checkpoint_service.exception.LocationNotFoundException
     *         si la localisation n'existe pas
     * @throws com.cbs.logistics.security_checkpoint_service.exception.LocationServiceUnavailableException
     *         si le Location Service est indisponible
     */
    LocationAvailability getLocation(String locationId);

    /**
     * Resultat mis en cache Redis : doit être Serializable
     * (sérialisation Jdk par défaut du RedisCacheConfiguration).
     */
    record LocationAvailability(Long packageId, boolean checkpointAvailable) implements Serializable {}
}
