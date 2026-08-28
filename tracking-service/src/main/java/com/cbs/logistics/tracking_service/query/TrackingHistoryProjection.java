package com.cbs.logistics.tracking_service.query;

import com.cbs.logistics.tracking_service.event.TrackingTransitionedEvent;
import lombok.extern.slf4j.Slf4j;
import org.axonframework.eventhandling.EventHandler;
import org.axonframework.config.ProcessingGroup;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Component;

/**
 * La PROJECTION — le côté LECTURE du CQRS.
 *
 * <p>Cette classe écoute l'EventBus et maintient la table tracking_history
 * en reflet de tous les événements. Elle ne contient AUCUNE règle métier :
 * les règles ont déjà été appliquées par l'aggregate AVANT que l'événement
 * n'existe. Une projection fait confiance aux événements qu'elle reçoit.</p>
 *
 * <p>La contrainte unique (package_id, status, occurred_at) empêche les
 * doublons lors du rebuild (rejeu des événements). Si l'entrée existe déjà,
 * l'exception est catchée et ignorée (idempotence).</p>
 */
@Slf4j
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
     * reconstruit toute seule ("rebuild").
     *
     * <p>Idempotent : si l'entrée existe déjà (rebuild), la contrainte unique
     * lève une DataIntegrityViolationException qui est catchée et ignorée.</p>
     */
    @EventHandler
    public void on(TrackingTransitionedEvent event) {
        try {
            repository.save(new TrackingHistoryEntry(
                    event.packageId(),
                    event.locationId(),
                    event.newStatus(),
                    event.occurredAt()
            ));
        } catch (DataIntegrityViolationException e) {
            // Doublon lors du rebuild : l'entrée existe déjà, on ignore
            log.debug("Duplicate event ignored: package {} status {} at {}",
                    event.packageId(), event.newStatus(), event.occurredAt());
        }
    }
}
