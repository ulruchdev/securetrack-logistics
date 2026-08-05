package com.cbs.logistics.security_checkpoint_service.controller;

import com.cbs.logistics.security_checkpoint_service.dto.CheckpointLogDto;
import com.cbs.logistics.security_checkpoint_service.dto.CreateCheckpointRequest;
import com.cbs.logistics.security_checkpoint_service.service.CheckpointLogService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/api/checkpoints")
@RequiredArgsConstructor
@Tag(name = "Checkpoint Logs", description = "Gestion des logs de passage aux checkpoints")
@SecurityRequirement(name = "basicAuth")
public class CheckpointLogController {

    private final CheckpointLogService service;

    @PostMapping
    @Operation(summary = "Enregistrer un passage", description = "Créer un nouveau log de passage de colis au checkpoint")
    @ApiResponse(responseCode = "201", description = "Passage enregistré avec succès")
    public ResponseEntity<CheckpointLogDto> createCheckpointLog(@Valid @RequestBody CreateCheckpointRequest request) {
        CheckpointLogDto dto = service.create(request);
        URI location = URI.create("/api/checkpoints/" + dto.id());
        return ResponseEntity.created(location).body(dto);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Consulter un log", description = "Récupérer les détails d'un passage spécifique")
    public ResponseEntity<CheckpointLogDto> getCheckpointLogById(@PathVariable Long id) {
        CheckpointLogDto dto = service.getById(id);
        return ResponseEntity.ok(dto);
    }

    @GetMapping
    @Operation(summary = "Lister les logs", description = "Lister tous les passages avec pagination")
    public ResponseEntity<Page<CheckpointLogDto>> getAllCheckpointLogs(Pageable pageable) {
        Page<CheckpointLogDto> logs = service.getAll(pageable);
        return ResponseEntity.ok(logs);
    }

    @GetMapping("/by-package/{packageId}")
    @Operation(summary = "Logs par colis", description = "Voir tous les checkpoints par lesquels un colis est passé")
    public ResponseEntity<List<CheckpointLogDto>> getCheckpointLogsByPackageId(@PathVariable Long packageId) {
        List<CheckpointLogDto> logs = service.getByPackageId(packageId);
        return ResponseEntity.ok(logs);
    }
}