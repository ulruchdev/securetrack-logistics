package com.cbs.logistics.location_service.repository;

import com.cbs.logistics.location_service.entity.Site;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface SiteRepository extends JpaRepository<Site, Long> {

    Page<Site> findByTenantId(String tenantId, Pageable pageable);

    Optional<Site> findByIdAndTenantId(Long id, String tenantId);

    List<Site> findByTenantIdAndActiveTrue(String tenantId);
}
