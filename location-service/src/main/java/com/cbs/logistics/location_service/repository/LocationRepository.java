package com.cbs.logistics.location_service.repository;

import org.springframework.data.mongodb.repository.MongoRepository;
import com.cbs.logistics.location_service.entity.Location;

import java.util.Optional;

public interface LocationRepository extends MongoRepository<Location, String> {

    Optional<Location> findByPackageId(Long packageId);
}
