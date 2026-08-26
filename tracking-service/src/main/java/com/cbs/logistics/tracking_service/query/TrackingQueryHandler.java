package com.cbs.logistics.tracking_service.query;

import com.cbs.logistics.tracking_service.exception.NotFoundException;
import org.axonframework.queryhandling.QueryHandler;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Handlers du côté LECTURE. Chaque @QueryHandler répond à un type de Query
 * via le QueryBus — l'équivalent lecture des @CommandHandler.
 */
@Component
public class TrackingQueryHandler {

    private final TrackingHistoryRepository repository;

    public TrackingQueryHandler(TrackingHistoryRepository repository) {
        this.repository = repository;
    }

    /** Historique complet d'un colis, trié chronologiquement. */
    @QueryHandler
    public List<TransitionDto> handle(FindHistoryQuery query) {
        return repository.findByPackageIdOrderByOccurredAtAsc(query.packageId()).stream()
                .map(TransitionDto::from)
                .toList();
    }

    /** Une transition précise ; 404 si inconnue. */
    @QueryHandler
    public TransitionDto handle(FindTransitionByIdQuery query) {
        return repository.findById(query.trackingId())
                .map(TransitionDto::from)
                .orElseThrow(() -> new NotFoundException(
                        "Aucune transition avec trackingId=" + query.trackingId()));
    }
}
