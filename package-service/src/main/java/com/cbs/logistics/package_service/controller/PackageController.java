package com.cbs.logistics.package_service.controller;

import com.cbs.logistics.common.dto.PackageDto;
import com.cbs.logistics.package_service.dto.CreatePackageRequest;
import com.cbs.logistics.package_service.dto.UpdatePackageRequest;
import com.cbs.logistics.package_service.service.PackageService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.Set;

@RestController
@RequestMapping("/api/packages")
@RequiredArgsConstructor
@Tag(name = "Packages", description = "Gestion des colis")
public class PackageController {

    private final PackageService packageService;

    /** Propriétés persistées autorisées pour le tri (whitelist anti-erreurs 500 / injection de tri). */
    private static final Set<String> ALLOWED_SORT_FIELDS = Set.of(
            "packageId", "trackingNumber", "packageName", "packageType", "description", "weight", "fragile", "packageStatus", "locationId");

    @Operation(summary = "Creer un colis", description = "Ajouter un nouveau colis avec statut initial NEW et trackingNumber généré automatiquement (ST-XXXXXXXX)")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Colis cree avec succes"),
            @ApiResponse(responseCode = "400", description = "Erreur de validation")
    })
    @PostMapping
    public ResponseEntity<PackageDto> createPackage(@Valid @RequestBody CreatePackageRequest request) {
        PackageDto packageDto = packageService.create(request);
        URI location = URI.create("/api/packages/" + packageDto.packageId());
        return ResponseEntity.created(location).body(packageDto);
    }

    @Operation(summary = "Consulter un colis", description = "Recuperer les details d'un colis par son identifiant")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Colis trouve"),
            @ApiResponse(responseCode = "404", description = "Colis introuvable")
    })
    @GetMapping("/{id}")
    public ResponseEntity<PackageDto> getPackageById(@PathVariable Long id) {
        PackageDto packageDto = packageService.getById(id);
        return ResponseEntity.ok(packageDto);
    }

    @Operation(summary = "Consulter par tracking number", description = "Rechercher un colis par son numéro de suivi ST-XXXXXXXX")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Colis trouve"),
            @ApiResponse(responseCode = "404", description = "Colis introuvable")
    })
    @GetMapping("/tracking/{trackingNumber}")
    public ResponseEntity<PackageDto> getPackageByTrackingNumber(@PathVariable String trackingNumber) {
        PackageDto packageDto = packageService.getByTrackingNumber(trackingNumber);
        return ResponseEntity.ok(packageDto);
    }

    @Operation(summary = "Lister les colis", description = "Liste paginee de tous les colis avec tri")
    @GetMapping
    public ResponseEntity<Page<PackageDto>> getAllPackages(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "packageId") String sortBy,
            @RequestParam(defaultValue = "asc") String sortDir
    ) {
        // Bornes de pagination (protection contre les requêtes abusives - CWE-400)
        if (page < 0) {
            throw new IllegalArgumentException("Le paramètre 'page' doit être positif ou nul");
        }
        if (size < 1 || size > 100) {
            throw new IllegalArgumentException("Le paramètre 'size' doit être compris entre 1 et 100");
        }

        // Tri : direction et champ autorisés uniquement (sinon erreur 500 au tri Spring Data)
        Sort.Direction direction;
        if (sortDir.equalsIgnoreCase("asc")) {
            direction = Sort.Direction.ASC;
        } else if (sortDir.equalsIgnoreCase("desc")) {
            direction = Sort.Direction.DESC;
        } else {
            throw new IllegalArgumentException("Le paramètre 'sortDir' doit être 'asc' ou 'desc'");
        }
        if (!ALLOWED_SORT_FIELDS.contains(sortBy)) {
            throw new IllegalArgumentException("Le paramètre 'sortBy' est invalide : " + sortBy);
        }

        Pageable pageable = PageRequest.of(page, size, Sort.by(direction, sortBy));
        Page<PackageDto> packages = packageService.getAll(pageable);
        return ResponseEntity.ok(packages);
    }

    @Operation(summary = "Mettre a jour un colis", description = "Modification partielle (PATCH) du statut ou des attributs")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Colis mis a jour"),
            @ApiResponse(responseCode = "404", description = "Colis introuvable"),
            @ApiResponse(responseCode = "409", description = "Transition de statut invalide")
    })
    @PatchMapping("/{id}")
    public ResponseEntity<PackageDto> updatePackage(@PathVariable Long id, @Valid @RequestBody UpdatePackageRequest request) {
        PackageDto packageDto = packageService.update(id, request);
        return ResponseEntity.ok(packageDto);
    }

    @Operation(summary = "Supprimer un colis", description = "Suppression definitive d'un colis")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Colis supprime"),
            @ApiResponse(responseCode = "404", description = "Colis introuvable")
    })
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deletePackage(@PathVariable Long id) {
        packageService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
