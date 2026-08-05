package com.cbs.logistics.location_service.client;

import com.cbs.logistics.location_service.dto.PackageDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(name = "package-service", url = "http://localhost:8081")
public interface PackageServiceClient {

    @GetMapping("/api/packages/{id}")
    PackageDto getPackageById(@PathVariable Long id);
}