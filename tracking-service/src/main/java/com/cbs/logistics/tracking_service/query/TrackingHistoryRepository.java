package com.cbs.logistics.tracking_service.query;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TrackingHistoryRepository extends JpaRepository<TrackingHistoryEntry, Long> {

    List<TrackingHistoryEntry> findByPackageIdOrderByOccurredAtAsc(String packageId);

    List<TrackingHistoryEntry> findByTenantIdOrderByOccurredAtAsc(String tenantId);

    List<TrackingHistoryEntry> findByPackageIdAndTenantIdOrderByOccurredAtAsc(String packageId, String tenantId);
}
