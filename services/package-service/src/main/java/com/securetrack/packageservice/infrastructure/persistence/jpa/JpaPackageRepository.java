package com.securetrack.packageservice.infrastructure.persistence.jpa;

import com.securetrack.packageservice.infrastructure.persistence.entity.JpaPackageEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface JpaPackageRepository extends JpaRepository<JpaPackageEntity, String> {
}