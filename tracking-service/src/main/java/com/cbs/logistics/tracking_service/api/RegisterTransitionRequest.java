package com.cbs.logistics.tracking_service.api;

import jakarta.validation.constraints.NotBlank;

/**
 * Corps de requête HTTP du POST /api/tracking.
 * La validation Bean Validation (@NotBlank) s'applique AVANT que la commande
 * ne parte sur le CommandBus : les erreurs de format sont rejetées en 400
 * sans solliciter le domaine.
 */
public record RegisterTransitionRequest(
        @NotBlank(message = "packageId est obligatoire")
        String packageId,

        String locationId,

        @NotBlank(message = "newStatus est obligatoire")
        String newStatus
) {
}
