package com.cbs.logistics.location_service.repository;

import com.cbs.logistics.location_service.entity.Location;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.Optional;

public interface LocationRepository extends MongoRepository<Location, String> {

    Optional<Location> findByPackageId(Long packageId);

    Page<Location> findByTenantId(String tenantId, Pageable pageable);

    Optional<Location> findByLocationIdAndTenantId(String locationId, String tenantId);

    Optional<Location> findByPackageIdAndTenantId(Long packageId, String tenantId);
}
