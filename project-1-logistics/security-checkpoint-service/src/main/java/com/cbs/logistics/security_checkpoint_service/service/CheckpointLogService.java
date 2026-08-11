package com.cbs.logistics.security_checkpoint_service.service;

import com.cbs.logistics.security_checkpoint_service.client.LocationServiceClient;
import com.cbs.logistics.security_checkpoint_service.dto.CheckpointLogDto;
import com.cbs.logistics.security_checkpoint_service.dto.CreateCheckpointRequest;
import com.cbs.logistics.security_checkpoint_service.entity.CheckpointLog;
import com.cbs.logistics.security_checkpoint_service.exception.CheckpointLogNotFoundException;
import com.cbs.logistics.security_checkpoint_service.exception.CheckpointUnavailableException;
import com.cbs.logistics.security_checkpoint_service.mapper.CheckpointLogMapper;
import com.cbs.logistics.security_checkpoint_service.repository.CheckpointLogRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;


@Service
@RequiredArgsConstructor
public class CheckpointLogService {

    private final CheckpointLogRepository repository;
    private final CheckpointLogMapper mapper;
    private final LocationServiceClient locationServiceClient;

    public CheckpointLogDto create(CreateCheckpointRequest request) {

        // Valider que la location existe et autorise les checkpoints
        // (le client Feign lève les exceptions métier via l'ErrorDecoder)
        LocationServiceClient.LocationDto location = locationServiceClient.getLocationById(request.getLocationId());
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