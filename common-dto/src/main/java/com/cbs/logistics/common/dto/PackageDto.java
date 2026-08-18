package com.cbs.logistics.common.dto;

/**
 * DTO partagé du contrat Package Service (réponse GET /api/packages/{id}).
 *
 * Source unique de vérité entre package-service (producteur) et location-service
 * (consommateur via Feign). packageStatus est une String (le contrat JSON est une
 * chaîne) — la conversion enum -> String est faite par MapStruct côté producteur.
 */
public record PackageDto(
        Long packageId,
        String description,
        String packageName,
        String packageType,
        Double weight,
        boolean fragile,
        String packageStatus
) {
}
