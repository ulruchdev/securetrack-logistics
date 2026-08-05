package com.cbs.logistics.location_service.service;

import com.cbs.logistics.location_service.client.PackageServiceClient;
import com.cbs.logistics.location_service.dto.CreateLocationRequest;
import com.cbs.logistics.location_service.dto.EnrichedLocationDto;
import com.cbs.logistics.location_service.dto.LocationDto;
import com.cbs.logistics.location_service.dto.PackageDto;
import com.cbs.logistics.location_service.entity.Location;
import com.cbs.logistics.location_service.exception.LocationNotFoundException;
import com.cbs.logistics.location_service.locationMapper.LocationMapper;
import com.cbs.logistics.location_service.repository.LocationRepository;
import feign.FeignException;
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

    public LocationDto create(CreateLocationRequest request) {
        // Valider que le package existe
        try {
            packageServiceClient.getPackageById(request.getPackageId());
        } catch (FeignException e) {
            if (e.status() == 404) {
                throw new IllegalArgumentException("Package avec id " + request.getPackageId() + "  n'exist");
            }
            throw new RuntimeException("Error validating package: " + e.getMessage(), e);
        }

        Location location = locationMapper.toEntity(request);
        Location saved = locationRepository.save(location);
        return locationMapper.toDto(saved);
    }

    public LocationDto getById(String id) {
        Location location = locationRepository.findById(id).orElseThrow(() -> new LocationNotFoundException("Location not found with id: " + id));
        return locationMapper.toDto(location);
    }

    public Page<LocationDto> getAll(Pageable pageable) {
        return locationRepository.findAll(pageable).map(locationMapper::toDto);
    }

    public EnrichedLocationDto getByPackageId(Long packageId) {
        Location location = locationRepository.findByPackageId(packageId)
                .orElseThrow(() -> new LocationNotFoundException("Location not found for package id: " + packageId));
        LocationDto locationDto = locationMapper.toDto(location);
        try {
            PackageDto packageDto = packageServiceClient.getPackageById(packageId);
            return new EnrichedLocationDto(locationDto, packageDto);
        } catch (FeignException e) {
            throw new RuntimeException("Error retrieving package information: " + e.getMessage(), e);
        }
    }
}