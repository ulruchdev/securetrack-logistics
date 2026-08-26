package com.cbs.logistics.tracking_service.event;

import java.time.Instant;

/**
 * Événement métier : FAIT ACCOMPLI, publié par l'agrégat après décision.
 *
 * <p>Règles absolues d'un événement en Event Sourcing :</p>
 * <ul>
 *   <li>Toujours formulé au PASSÉ ("a transité"), jamais au présent ;</li>
 *   <li>Immuable (record) : il sera rejoué tel quel pendant toute la vie
 *       du système pour reconstruire les états ;</li>
 *   <li>Il contient toutes les informations nécessaires aux consommateurs
 *       (ici : la projection), sans qu'ils aient à re-interroger quoi que
 *       ce soit.</li>
 * </ul>
 *
 * <p>Aucune annotation Axon ici : un événement est un simple message de
 * données. C'est le contexte (@EventSourcingHandler / @EventHandler) qui
 * détermine qui le reçoit.</p>
 *
 * @param packageId  colis concerné (identifiant de l'aggregate)
 * @param locationId lieu de la transition (peut être null)
 * @param newStatus  nouveau statut après transition
 * @param occurredAt horodatage de l'événement
 */
public record TrackingTransitionedEvent(
        String packageId,
        String locationId,
        String newStatus,
        Instant occurredAt
) {
}
