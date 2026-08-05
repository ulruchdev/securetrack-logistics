package com.cbs.logistics.package_service.repository;

import com.cbs.logistics.package_service.entity.Package;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PackageRepository extends JpaRepository<Package ,Long>{
}
