package com.cbs.logistics.location_service.controller;

import com.cbs.logistics.location_service.dto.CheckpointDto;
import com.cbs.logistics.location_service.dto.CreateCheckpointRequest;
import com.cbs.logistics.location_service.service.CheckpointService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/checkpoints")
@RequiredArgsConstructor
@Tag(name = "Checkpoints", description = "Gestion des points de contrôle")
public class CheckpointController {

    private final CheckpointService checkpointService;

    @PostMapping
    @Operation(summary = "Créer un checkpoint", description = "Crée un nouveau checkpoint dans un site")
    @ApiResponse(responseCode = "201", description = "Checkpoint créé avec succès")
    public ResponseEntity<CheckpointDto> create(@Valid @RequestBody CreateCheckpointRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(checkpointService.create(request));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Récupérer un checkpoint par ID")
    public ResponseEntity<CheckpointDto> getById(@PathVariable Long id) {
        return ResponseEntity.ok(checkpointService.getById(id));
    }

    @GetMapping
    @Operation(summary = "Lister tous les checkpoints du tenant")
    public ResponseEntity<Page<CheckpointDto>> getAll(Pageable pageable) {
        return ResponseEntity.ok(checkpointService.getAll(pageable));
    }

    @GetMapping("/by-site/{siteId}")
    @Operation(summary = "Lister les checkpoints d'un site")
    public ResponseEntity<List<CheckpointDto>> getBySiteId(@PathVariable Long siteId) {
        return ResponseEntity.ok(checkpointService.getBySiteId(siteId));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Mettre à jour un checkpoint")
    public ResponseEntity<CheckpointDto> update(@PathVariable Long id, @Valid @RequestBody CreateCheckpointRequest request) {
        return ResponseEntity.ok(checkpointService.update(id, request));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Désactiver un checkpoint (soft delete)")
    @ApiResponse(responseCode = "204", description = "Checkpoint désactivé")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        checkpointService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
