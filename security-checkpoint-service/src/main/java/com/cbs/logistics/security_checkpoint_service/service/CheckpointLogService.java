package com.cbs.logistics.security_checkpoint_service.service;

import com.cbs.logistics.security_checkpoint_service.dto.CheckpointLogDto;
import com.cbs.logistics.security_checkpoint_service.dto.CreateCheckpointRequest;
import com.cbs.logistics.security_checkpoint_service.entity.CheckpointLog;
import com.cbs.logistics.security_checkpoint_service.exception.CheckpointLogNotFoundException;
import com.cbs.logistics.security_checkpoint_service.exception.CheckpointUnavailableException;
import com.cbs.logistics.security_checkpoint_service.exception.LocationPackageMismatchException;
import com.cbs.logistics.security_checkpoint_service.mapper.CheckpointLogMapper;
import com.cbs.logistics.security_checkpoint_service.port.LocationAvailabilityPort;
import com.cbs.logistics.security_checkpoint_service.repository.CheckpointLogRepository;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;


@Service
@RequiredArgsConstructor
public class CheckpointLogService {

    private final CheckpointLogRepository repository;
    private final CheckpointLogMapper mapper;
    private final LocationAvailabilityPort locationAvailabilityPort;

    @CircuitBreaker(name = "locationService")
    public CheckpointLogDto create(CreateCheckpointRequest request) {

        // Valider que la location existe (via le port applicatif — l'adapter Feign
        // encapsule le client HTTP et gère le cache Redis ; les exceptions métier
        // sont levées par l'ErrorDecoder)
        LocationAvailabilityPort.LocationAvailability location =
                locationAvailabilityPort.getLocation(request.getLocationId());

        // Intégrité : le colis du checkpoint doit être celui rattaché à la localisation
        // (null côté location = champ absent → refusé, on ne peut pas prouver la concordance)
        if (!java.util.Objects.equals(location.packageId(), request.getPackageId())) {
            throw new LocationPackageMismatchException(request.getLocationId(), request.getPackageId(), location.packageId());
        }

        // Valider que la localisation autorise les checkpoints
        if (!location.checkpointAvailable()) {
            throw new CheckpointUnavailableException("Checkpoint not available for location: " + request.getLocationId());
        }

        CheckpointLog entity = mapper.toEntity(request);
        CheckpointLog saved = repository.save(entity);
        return mapper.toDto(saved);
    }

    public CheckpointLogDto getById(Long id) {
        CheckpointLog entity = repository.findById(id)
                .orElseThrow(() -> new CheckpointLogNotFoundException(id));
        return mapper.toDto(entity);
    }

    public Page<CheckpointLogDto> getAll(Pageable pageable) {
        return repository.findAll(pageable).map(mapper::toDto);
    }

    public Page<CheckpointLogDto> getByPackageId(Long packageId, Pageable pageable) {
        return repository.findByPackageIdOrderByCheckpointTimeDesc(packageId, pageable).map(mapper::toDto);
    }
}