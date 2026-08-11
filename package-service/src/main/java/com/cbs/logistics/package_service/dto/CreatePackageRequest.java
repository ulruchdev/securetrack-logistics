package com.cbs.logistics.package_service.dto;

import com.cbs.logistics.package_service.entity.PackageStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request pour la création d'un Package.
 * Champs obligatoires pour un CREATE complet.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreatePackageRequest {

    @NotBlank(message = "La description est obligatoire")
    private String description;

    @NotBlank(message = "Le nom du colis est obligatoire")
    private String packageName;

    @NotBlank(message = "Le type de colis est obligatoire")
    private String packageType;

    @NotNull(message = "Le poids est obligatoire")
    @PositiveOrZero(message = "Le poids doit être positif ou nul")
    private Double weight;

    @NotNull(message = "L'indicateur de fragilité est obligatoire")
    private Boolean fragile;



}