package com.cbs.logistics.security_checkpoint_service.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(name = "location-service", url = "http://localhost:8082")
public interface LocationServiceClient {

    @GetMapping("/api/locations/{id}")
    LocationDto getLocationById(@PathVariable String id);

    record LocationDto(String city, String zone, Boolean checkpointAvailable) {}
}