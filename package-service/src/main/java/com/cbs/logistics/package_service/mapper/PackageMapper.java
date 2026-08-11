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

    /** Update partiel : applique les champs non-null du request sur l'entity existante.
     *  Les champs null du request sont IGNORÉS (pas de SET_TO_NULL) pour ne pas écraser
     *  les valeurs existantes lors d'un PATCH partiel. */
    @Mapping(target = "packageId", ignore = true)
    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    void updateEntityFromRequest(UpdatePackageRequest request, @MappingTarget Package entity);
}