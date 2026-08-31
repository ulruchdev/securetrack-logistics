package com.cbs.logistics.security_checkpoint_service.repository;

import com.cbs.logistics.security_checkpoint_service.entity.CheckpointLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CheckpointLogRepository extends JpaRepository<CheckpointLog, Long> {

    Page<CheckpointLog> findByTenantId(String tenantId, Pageable pageable);

    Page<CheckpointLog> findByTrackingNumberAndTenantIdOrderByCheckpointTimeDesc(
            String trackingNumber, String tenantId, Pageable pageable);
}
