package com.cbs.logistics.tracking_service.query;

import java.time.Instant;

/**
 * DTO de réponse : ce que le client voit. Découplé de l'entité JPA pour que
 * le schéma de la table puisse évoluer sans casser l'API publique.
 */
public record TransitionDto(
        Long trackingId,
        String packageId,
        String locationId,
        String status,
        Instant occurredAt
) {

    static TransitionDto from(TrackingHistoryEntry entry) {
        return new TransitionDto(
                entry.getTrackingId(),
                entry.getPackageId(),
                entry.getLocationId(),
                entry.getStatus(),
                entry.getOccurredAt()
        );
    }
}
