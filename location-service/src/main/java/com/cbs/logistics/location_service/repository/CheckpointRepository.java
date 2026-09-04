package com.cbs.logistics.location_service.repository;

import com.cbs.logistics.location_service.entity.Checkpoint;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CheckpointRepository extends JpaRepository<Checkpoint, Long> {

    Page<Checkpoint> findByTenantId(String tenantId, Pageable pageable);

    List<Checkpoint> findBySiteIdAndTenantId(Long siteId, String tenantId);

    Optional<Checkpoint> findByIdAndTenantId(Long id, String tenantId);

    List<Checkpoint> findByTenantIdAndActiveTrue(String tenantId);
}
