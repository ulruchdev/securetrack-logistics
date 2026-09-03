package com.cbs.logistics.location_service.service;

import com.cbs.logistics.common.dto.PackageDto;
import com.cbs.logistics.common.security.context.TenantContext;
import com.cbs.logistics.location_service.client.PackageServiceClient;
import com.cbs.logistics.location_service.dto.CreateLocationRequest;
import com.cbs.logistics.location_service.dto.EnrichedLocationDto;
import com.cbs.logistics.location_service.dto.LocationDto;
import com.cbs.logistics.location_service.entity.Location;
import com.cbs.logistics.location_service.exception.LocationNotFoundException;
import com.cbs.logistics.location_service.locationMapper.LocationMapper;
import com.cbs.logistics.location_service.repository.LocationRepository;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class LocationService {

    private final LocationRepository locationRepository;
    private final LocationMapper locationMapper;
    private final PackageServiceClient packageServiceClient;

    @CircuitBreaker(name = "packageService")
    @Retry(name = "packageService")
    public LocationDto create(CreateLocationRequest request) {
        packageServiceClient.getPackageById(request.getPackageId());

        Location location = locationMapper.toEntity(request);
        location.setTenantId(TenantContext.getCurrent());
        Location saved = locationRepository.save(location);
        return locationMapper.toDto(saved);
    }

    public LocationDto getById(String id) {
        String tenantId = TenantContext.getCurrent();
        Location location = locationRepository.findByLocationIdAndTenantId(id, tenantId)
                .orElseThrow(() -> new LocationNotFoundException("Location not found with id: " + id));
        return locationMapper.toDto(location);
    }

    public Page<LocationDto> getAll(Pageable pageable) {
        String tenantId = TenantContext.getCurrent();
        return locationRepository.findByTenantId(tenantId, pageable).map(locationMapper::toDto);
    }

    @CircuitBreaker(name = "packageService")
    @Retry(name = "packageService")
    public EnrichedLocationDto getByPackageId(Long packageId) {
        String tenantId = TenantContext.getCurrent();
        Location location = locationRepository.findByPackageIdAndTenantId(packageId, tenantId)
                .orElseThrow(() -> new LocationNotFoundException("Location not found for package id: " + packageId));
        LocationDto locationDto = locationMapper.toDto(location);
        PackageDto packageDto = packageServiceClient.getPackageById(packageId);
        return new EnrichedLocationDto(locationDto, packageDto);
    }
}
