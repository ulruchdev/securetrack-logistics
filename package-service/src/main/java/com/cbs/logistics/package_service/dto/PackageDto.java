package com.cbs.logistics.package_service.dto;

import com.cbs.logistics.package_service.entity.PackageStatus;

/**
 * DTO immutable pour la réponse API (lecture).
 * Utilisation d'un record : concis, immutable, equals/hashCode/toString automatiques.
 */
public record PackageDto(
        Long packageId,
        String description,
        String packageName,
        String packageType,
        Double weight,
        boolean fragile,
        PackageStatus packageStatus
) {
}