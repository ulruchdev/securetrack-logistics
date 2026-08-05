package com.cbs.logistics.package_service.dto;

import com.cbs.logistics.package_service.entity.PackageStatus;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request pour la mise à jour partielle d'un Package (PATCH).
 * Tous les champs sont optionnels → permet des updates partielles.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdatePackageRequest {

    private String description;

    private String packageName;

    private String packageType;

    @PositiveOrZero(message = "Weight must be positive or zero")
    private Double weight;

    private Boolean fragile;

    private PackageStatus packageStatus;
}