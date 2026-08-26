package com.cbs.logistics.tracking_service.query;

import com.cbs.logistics.tracking_service.event.TrackingTransitionedEvent;
import org.axonframework.eventhandling.EventHandler;
import org.axonframework.config.ProcessingGroup;
import org.springframework.stereotype.Component;

/**
 * La PROJECTION — le côté LECTURE du CQRS.
 *
 * <p>Cette classe écoute l'EventBus et maintient la table tracking_history
 * en reflet de tous les événements. Elle ne contient AUCUNE règle métier :
 * les règles ont déjà été appliquées par l'aggregate AVANT que l'événement
 * n'existe. Une projection fait confiance aux événements qu'elle reçoit.</p>
 */
@Component
@ProcessingGroup("tracking-history")
public class TrackingHistoryProjection {

    private final TrackingHistoryRepository repository;

    public TrackingHistoryProjection(TrackingHistoryRepository repository) {
        this.repository = repository;
    }

    /**
     * Appelé par Axon pour chaque TrackingTransitionedEvent publié sur
     * l'EventBus — y compris (et surtout) pour les événements passés : au
     * premier démarrage, Axon REJOUE tout l'event store et la projection se
     * reconstruit toute seule ("rebuild"). C'est un super-pouvoir de
     * l'event sourcing : on peut changer le read-model et le remplir
     * simplement en redémarrant.
     */
    @EventHandler
    public void on(TrackingTransitionedEvent event) {
        repository.save(new TrackingHistoryEntry(
                event.packageId(),
                event.locationId(),
                event.newStatus(),
                event.occurredAt()
        ));
    }
}
