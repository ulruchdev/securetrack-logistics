package com.cbs.logistics.package_service.repository;

import com.cbs.logistics.package_service.entity.Package;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PackageRepository extends JpaRepository<Package, Long> {

    Page<Package> findByTenantIdAndDeletedAtIsNull(String tenantId, Pageable pageable);

    Optional<Package> findByPackageIdAndTenantIdAndDeletedAtIsNull(Long packageId, String tenantId);

    boolean existsByPackageIdAndTenantIdAndDeletedAtIsNull(Long packageId, String tenantId);

    Optional<Package> findByTrackingNumberAndTenantIdAndDeletedAtIsNull(String trackingNumber, String tenantId);
}
