package com.cbs.logistics.security_checkpoint_service.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(name = "location-service", url = "${location-service.url:http://localhost:8082}")
public interface LocationServiceClient {

    @GetMapping("/api/checkpoints/{id}")
    CheckpointDto getCheckpointById(@PathVariable Long id);

    /**
     * DTO du contrat Location Service (GET /api/checkpoints/{id}).
     * Utilisé pour vérifier qu'un checkpoint existe et est actif
     * avant d'enregistrer un scan.
     */
    record CheckpointDto(Long id, Long siteId, String name, boolean active) {}
}
