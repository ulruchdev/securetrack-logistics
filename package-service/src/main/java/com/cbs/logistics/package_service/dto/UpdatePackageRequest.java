package com.cbs.logistics.package_service.dto;

import com.cbs.logistics.package_service.entity.PackageStatus;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request pour la mise à jour partielle d'un Package (PATCH).
 * Tous les champs sont optionnels → null = champ non modifié (PATCH partiel).
 * Les champs fournis ne doivent toutefois pas être vides (blank rejeté).
 * NB : on utilise @Pattern plutôt que @NotBlank car @NotBlank rejette null,
 * ce qui casserait le PATCH partiel — @Pattern ignore null et rejette le blank.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdatePackageRequest {

    @Pattern(regexp = "\\S.*", message = "La description ne peut pas être vide")
    private String description;

    @Pattern(regexp = "\\S.*", message = "Le nom du colis ne peut pas être vide")
    private String packageName;

    @Pattern(regexp = "\\S.*", message = "Le type de colis ne peut pas être vide")
    private String packageType;

    @PositiveOrZero(message = "Le poids doit être positif ou nul")
    private Double weight;

    private Boolean fragile;

    private PackageStatus packageStatus;
}