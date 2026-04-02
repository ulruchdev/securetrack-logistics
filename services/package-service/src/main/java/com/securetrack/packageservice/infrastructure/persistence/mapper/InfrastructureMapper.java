package com.securetrack.packageservice.infrastructure.persistence.mapper;

import com.securetrack.packageservice.domain.model.Package;
import com.securetrack.packageservice.infrastructure.persistence.entity.JpaPackageEntity;
import org.mapstruct.Mapper;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "spring")
public interface InfrastructureMapper {

    JpaPackageEntity toJpaEntity(Package domain);

    Package toDomainModel(JpaPackageEntity jpaEntity);

    void updateJpaEntity(@MappingTarget JpaPackageEntity jpaEntity, Package domain);
}