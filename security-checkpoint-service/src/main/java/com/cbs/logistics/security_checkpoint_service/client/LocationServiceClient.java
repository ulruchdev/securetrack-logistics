package com.cbs.logistics.security_checkpoint_service.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(name = "location-service", url = "${location-service.url:http://localhost:8082}")
public interface LocationServiceClient {

    @GetMapping("/api/locations/{id}")
    LocationDto getLocationById(@PathVariable String id);

    /**
     * DTO du contrat Location Service (GET /api/locations/{id}).
     * packageId : colis rattaché à la localisation (utilisé pour vérifier que le
     * checkpoint concerne bien le même colis que la requête).
     * checkpointAvailable : boolean primitif — si absent, désérialisation en false.
     */
    record LocationDto(String locationId, Long packageId, String city, String zone, boolean checkpointAvailable) {}
}