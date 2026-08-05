package com.cbs.logistics.package_service.controller;

import com.cbs.logistics.package_service.dto.CreatePackageRequest;
import com.cbs.logistics.package_service.dto.PackageDto;
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

@RestController
@RequestMapping("/api/packages")
@RequiredArgsConstructor
public class PackageController {

    private final PackageService packageService;

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
        Sort.Direction direction = sortDir.equalsIgnoreCase("desc") ? Sort.Direction.DESC : Sort.Direction.ASC;
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
