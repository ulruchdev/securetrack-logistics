package com.cbs.logistics.security_checkpoint_service.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(name = "package-service", url = "${package-service.url:http://localhost:8081}")
public interface PackageServiceClient {

    @GetMapping("/api/packages/tracking/{trackingNumber}")
    PackageDto getPackageByTrackingNumber(@PathVariable String trackingNumber);

    /**
     * DTO minimal du Package Service (GET /api/packages/tracking/{trackingNumber}).
     * Utilisé pour valider l'existence d'un colis par son numéro de suivi.
     */
    record PackageDto(Long packageId, String trackingNumber) {}
}
