package com.cbs.logistics.package_service.mapper;

import com.cbs.logistics.package_service.dto.CreatePackageRequest;
import com.cbs.logistics.package_service.dto.PackageDto;
import com.cbs.logistics.package_service.dto.UpdatePackageRequest;
import com.cbs.logistics.package_service.entity.Package;
import org.mapstruct.*;

/**
 * Mapper centralisé pour Package ↔ DTO.
 * componentModel = "spring" → injectable comme bean Spring.
 */
@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface PackageMapper {

    /** Entity → DTO pour les réponses API */
    PackageDto toDto(Package entity);

    /** Request Create → Entity (ignore l'ID qui est auto-généré) */
    @Mapping(target = "packageId", ignore = true)
    Package toEntity(CreatePackageRequest request);

    /** Update partiel : applique les champs non-null du request sur l'entity existante */
    @Mapping(target = "packageId", ignore = true)
    void updateEntityFromRequest(UpdatePackageRequest request, @MappingTarget Package entity);
}