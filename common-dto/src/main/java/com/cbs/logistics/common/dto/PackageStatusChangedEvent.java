package com.cbs.logistics.common.dto;

import java.io.Serializable;
import java.time.Instant;

/**
 * Événement métier partagé : publié par Package Service quand le statut
 * d'un colis change, consommé par Tracking Service pour enregistrer
 * automatiquement la transition.
 *
 * <p>Ce DTO est un record Java sérialisable en JSON via Jackson.
 * Il vit dans common-dto pour être accessible par les deux services
 * sans dépendance circulaire.</p>
 *
 * @param packageId      identifiant du colis
 * @param previousStatus statut avant la transition (null si création)
 * @param newStatus      statut après la transition
 * @param locationId     lieu de la transition (optionnel)
 * @param timestamp      horodatage de la transition
 */
public record PackageStatusChangedEvent(
        Long packageId,
        String previousStatus,
        String newStatus,
        Long locationId,
        Instant timestamp
) implements Serializable {
}
