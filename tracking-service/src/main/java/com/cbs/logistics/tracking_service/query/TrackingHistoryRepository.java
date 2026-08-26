package com.cbs.logistics.tracking_service.query;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/**
 * Repository de la projection — utilisé UNIQUEMENT par le côté lecture
 * (@EventHandler pour écrire, @QueryHandler pour lire).
 */
public interface TrackingHistoryRepository extends JpaRepository<TrackingHistoryEntry, Long> {

    List<TrackingHistoryEntry> findByPackageIdOrderByOccurredAtAsc(String packageId);
}
