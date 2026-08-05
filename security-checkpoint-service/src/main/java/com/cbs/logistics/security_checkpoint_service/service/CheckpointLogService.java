package com.cbs.logistics.security_checkpoint_service.service;

import com.cbs.logistics.security_checkpoint_service.client.LocationServiceClient;
import com.cbs.logistics.security_checkpoint_service.dto.CheckpointLogDto;
import com.cbs.logistics.security_checkpoint_service.dto.CreateCheckpointRequest;
import com.cbs.logistics.security_checkpoint_service.entity.CheckpointLog;
import com.cbs.logistics.security_checkpoint_service.exception.CheckpointLogNotFoundException;
import com.cbs.logistics.security_checkpoint_service.mapper.CheckpointLogMapper;
import com.cbs.logistics.security_checkpoint_service.repository.CheckpointLogRepository;
import feign.FeignException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CheckpointLogService {

    private final CheckpointLogRepository repository;
    private final CheckpointLogMapper mapper;
    private final LocationServiceClient locationServiceClient;

    public CheckpointLogDto create(CreateCheckpointRequest request) {
        // Valider que la location existe et autorise les checkpoints
        try {
            LocationServiceClient.LocationDto location = locationServiceClient.getLocationById(request.getLocationId());
            if (!location.checkpointAvailable()) {
                throw new IllegalArgumentException("Checkpoint not available for location: " + request.getLocationId());
            }
        } catch (FeignException e) {
            if (e.status() == 404) {
                throw new IllegalArgumentException("Location not found: " + request.getLocationId());
            }
            throw new RuntimeException("Error validating location: " + e.getMessage(), e);
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

    public List<CheckpointLogDto> getByPackageId(Long packageId) {
        return repository.findByPackageIdOrderByCheckpointTimeDesc(packageId)
                .stream()
                .map(mapper::toDto)
                .toList();
    }

    public Page<CheckpointLogDto> getByPackageId(Long packageId, Pageable pageable) {
        return repository.findByPackageId(packageId, pageable).map(mapper::toDto);
    }
}