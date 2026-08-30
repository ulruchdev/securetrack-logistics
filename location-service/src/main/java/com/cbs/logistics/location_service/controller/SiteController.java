package com.cbs.logistics.location_service.controller;

import com.cbs.logistics.location_service.dto.CreateSiteRequest;
import com.cbs.logistics.location_service.dto.SiteDto;
import com.cbs.logistics.location_service.service.SiteService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/sites")
@RequiredArgsConstructor
@Tag(name = "Sites", description = "Gestion des sites logistiques")
public class SiteController {

    private final SiteService siteService;

    @PostMapping
    @Operation(summary = "Créer un site", description = "Crée un nouveau site logistique pour le tenant connecté")
    @ApiResponse(responseCode = "201", description = "Site créé avec succès")
    @ApiResponse(responseCode = "400", description = "Données invalides")
    public ResponseEntity<SiteDto> create(@Valid @RequestBody CreateSiteRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(siteService.create(request));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Récupérer un site par ID")
    public ResponseEntity<SiteDto> getById(@PathVariable Long id) {
        return ResponseEntity.ok(siteService.getById(id));
    }

    @GetMapping
    @Operation(summary = "Lister tous les sites du tenant")
    public ResponseEntity<Page<SiteDto>> getAll(Pageable pageable) {
        return ResponseEntity.ok(siteService.getAll(pageable));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Mettre à jour un site")
    public ResponseEntity<SiteDto> update(@PathVariable Long id, @Valid @RequestBody CreateSiteRequest request) {
        return ResponseEntity.ok(siteService.update(id, request));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Désactiver un site (soft delete)")
    @ApiResponse(responseCode = "204", description = "Site désactivé")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        siteService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
