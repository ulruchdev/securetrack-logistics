package com.cbs.logistics.location_service.controller;

import com.cbs.logistics.location_service.dto.CreateLocationRequest;
import com.cbs.logistics.location_service.dto.EnrichedLocationDto;
import com.cbs.logistics.location_service.dto.LocationDto;
import com.cbs.logistics.location_service.service.LocationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/locations")
@RequiredArgsConstructor
@Tag(name = "Locations", description = "Gestion des emplacements logistiques")
public class LocationController {

    private final LocationService locationService;

    @Operation(summary = "Creer un emplacement", description = "Enregistrer un nouvel emplacement logistique")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Emplacement cree"),
            @ApiResponse(responseCode = "400", description = "Erreur de validation")
    })
    @PostMapping
    public ResponseEntity<LocationDto> createLocation(@Valid @RequestBody CreateLocationRequest request) {
        LocationDto locationDto = locationService.create(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(locationDto);
    }

    @Operation(summary = "Consulter un emplacement", description = "Recuperer les details d'un emplacement par son identifiant")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Emplacement trouve"),
            @ApiResponse(responseCode = "404", description = "Emplacement introuvable")
    })
    @GetMapping("/{id}")
    public ResponseEntity<LocationDto> getLocationById(@PathVariable String id) {
        LocationDto locationDto = locationService.getById(id);
        return ResponseEntity.ok(locationDto);
    }

    @Operation(summary = "Lister les emplacements", description = "Liste paginee de tous les emplacements")
    @GetMapping
    public ResponseEntity<Page<LocationDto>> getAllLocations(Pageable pageable) {
        Page<LocationDto> locations = locationService.getAll(pageable);
        return ResponseEntity.ok(locations);
    }

    @Operation(summary = "Emplacement par colis", description = "Trouver l'emplacement associe a un colis (verifie l'existence du colis via Package Service)")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Emplacement trouve"),
            @ApiResponse(responseCode = "404", description = "Colis ou emplacement introuvable"),
            @ApiResponse(responseCode = "503", description = "Package Service indisponible")
    })
    @GetMapping("/by-package/{packageId}")
    public ResponseEntity<EnrichedLocationDto> getLocationByPackageId(@PathVariable Long packageId) {
        EnrichedLocationDto enrichedLocationDto = locationService.getByPackageId(packageId);
        return ResponseEntity.ok(enrichedLocationDto);
    }
}