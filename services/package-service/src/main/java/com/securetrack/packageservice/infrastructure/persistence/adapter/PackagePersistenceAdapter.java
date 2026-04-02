package com.securetrack.packageservice.infrastructure.persistence.adapter;

import com.securetrack.packageservice.application.port.out.PackageRepositoryPort;
import com.securetrack.packageservice.domain.model.Package;
import com.securetrack.packageservice.infrastructure.persistence.entity.JpaPackageEntity;
import com.securetrack.packageservice.infrastructure.persistence.jpa.JpaPackageRepository;
import com.securetrack.packageservice.infrastructure.persistence.mapper.InfrastructureMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class PackagePersistenceAdapter implements PackageRepositoryPort {

    private final JpaPackageRepository jpaRepository;
    private final InfrastructureMapper infrastructureMapper;

    @Override
    public Package save(Package domainPackage) {
        JpaPackageEntity entity = infrastructureMapper.toJpaEntity(domainPackage);
        JpaPackageEntity savedEntity = jpaRepository.save(entity);
        return infrastructureMapper.toDomainModel(savedEntity);
    }
}