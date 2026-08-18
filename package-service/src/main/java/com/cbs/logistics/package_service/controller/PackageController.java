package com.cbs.logistics.package_service.controller;

import com.cbs.logistics.common.dto.PackageDto;
import com.cbs.logistics.package_service.dto.CreatePackageRequest;
import com.cbs.logistics.package_service.dto.UpdatePackageRequest;
import com.cbs.logistics.package_service.service.PackageService;
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
public class PackageController {

    private final PackageService packageService;

    /** Propriétés persistées autorisées pour le tri (whitelist anti-erreurs 500 / injection de tri). */
    private static final Set<String> ALLOWED_SORT_FIELDS = Set.of(
            "packageId", "packageName", "packageType", "description", "weight", "fragile", "packageStatus", "locationId");

    @PostMapping
    public ResponseEntity<PackageDto> createPackage(@Valid @RequestBody CreatePackageRequest request) {
        PackageDto packageDto = packageService.create(request);
        URI location = URI.create("/api/packages/" + packageDto.packageId());
        return ResponseEntity.created(location).body(packageDto);
    }

    @GetMapping("/{id}")
    public ResponseEntity<PackageDto> getPackageById(@PathVariable Long id) {
        PackageDto packageDto = packageService.getById(id);
        return ResponseEntity.ok(packageDto);
    }

    @GetMapping
    public ResponseEntity<Page<PackageDto>> getAllPackages(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "packageId") String sortBy,
            @RequestParam(defaultValue = "asc") String sortDir
            ){
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

    @PatchMapping("/{id}")
    public ResponseEntity<PackageDto> updatePackage(@PathVariable Long id, @Valid @RequestBody UpdatePackageRequest request) {
        PackageDto packageDto = packageService.update(id, request);
        return ResponseEntity.ok(packageDto);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deletePackage(@PathVariable Long id) {
        packageService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
