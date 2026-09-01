package com.cbs.logistics.package_service.repository;

import com.cbs.logistics.package_service.entity.EventOutbox;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface EventOutboxRepository extends JpaRepository<EventOutbox, Long> {

    List<EventOutbox> findByStatusOrderByCreatedAtAsc(String status);

    @Modifying
    @Query("UPDATE EventOutbox e SET e.status = 'PUBLISHED', e.publishedAt = CURRENT_TIMESTAMP WHERE e.id = :id")
    int markAsPublished(Long id);

    @Modifying
    @Query("UPDATE EventOutbox e SET e.status = 'FAILED', e.retryCount = e.retryCount + 1 WHERE e.id = :id")
    int markAsFailed(Long id);

    @Query("SELECT COUNT(e) FROM EventOutbox e WHERE e.status = 'FAILED' AND e.retryCount >= 5")
    long countPermanentFailures();
}
