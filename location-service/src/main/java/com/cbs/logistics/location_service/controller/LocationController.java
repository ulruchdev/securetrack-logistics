package com.cbs.logistics.location_service.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import com.cbs.logistics.location_service.service.LocationService;
import com.cbs.logistics.location_service.dto.LocationDto;
import com.cbs.logistics.location_service.dto.CreateLocationRequest;
import com.cbs.logistics.location_service.dto.EnrichedLocationDto;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/locations")
@RequiredArgsConstructor
public class LocationController {

    private final LocationService locationService;

    @PostMapping
    public ResponseEntity<LocationDto> createLocation(@Valid @RequestBody CreateLocationRequest request) {
        LocationDto locationDto = locationService.create(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(locationDto);
    }

    @GetMapping("/{id}")
    public ResponseEntity<LocationDto> getLocationById(@PathVariable String id) {
        LocationDto locationDto = locationService.getById(id);
        return ResponseEntity.ok(locationDto);
    }

    @GetMapping
    public ResponseEntity<Page<LocationDto>> getAllLocations(Pageable pageable) {
        Page<LocationDto> locations = locationService.getAll(pageable);
        return ResponseEntity.ok(locations);
    }

    @GetMapping("/by-package/{packageId}")
    public ResponseEntity<EnrichedLocationDto> getLocationByPackageId(@PathVariable Long packageId) {
        EnrichedLocationDto enrichedLocationDto = locationService.getByPackageId(packageId);
        return ResponseEntity.ok(enrichedLocationDto);
    }
}