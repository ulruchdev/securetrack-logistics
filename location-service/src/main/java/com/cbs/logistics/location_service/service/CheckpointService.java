package com.cbs.logistics.location_service.service;

import com.cbs.logistics.common.security.context.TenantContext;
import com.cbs.logistics.location_service.dto.CheckpointDto;
import com.cbs.logistics.location_service.dto.CreateCheckpointRequest;
import com.cbs.logistics.location_service.entity.Checkpoint;
import com.cbs.logistics.location_service.exception.LocationNotFoundException;
import com.cbs.logistics.location_service.repository.CheckpointRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CheckpointService {

    private final CheckpointRepository checkpointRepository;

    public CheckpointDto create(CreateCheckpointRequest request) {
        Checkpoint checkpoint = Checkpoint.builder()
                .tenantId(TenantContext.getCurrent())
                .siteId(request.siteId())
                .name(request.name())
                .active(true)
                .build();
        Checkpoint saved = checkpointRepository.save(checkpoint);
        return toDto(saved);
    }

    public CheckpointDto getById(Long id) {
        String tenantId = TenantContext.getCurrent();
        Checkpoint checkpoint = checkpointRepository.findByIdAndTenantId(id, tenantId)
                .orElseThrow(() -> new LocationNotFoundException("Checkpoint not found with id: " + id));
        return toDto(checkpoint);
    }

    public Page<CheckpointDto> getAll(Pageable pageable) {
        String tenantId = TenantContext.getCurrent();
        return checkpointRepository.findByTenantId(tenantId, pageable).map(this::toDto);
    }

    public List<CheckpointDto> getBySiteId(Long siteId) {
        String tenantId = TenantContext.getCurrent();
        return checkpointRepository.findBySiteIdAndTenantId(siteId, tenantId)
                .stream().map(this::toDto).toList();
    }

    public CheckpointDto update(Long id, CreateCheckpointRequest request) {
        String tenantId = TenantContext.getCurrent();
        Checkpoint checkpoint = checkpointRepository.findByIdAndTenantId(id, tenantId)
                .orElseThrow(() -> new LocationNotFoundException("Checkpoint not found with id: " + id));
        checkpoint.setSiteId(request.siteId());
        checkpoint.setName(request.name());
        Checkpoint saved = checkpointRepository.save(checkpoint);
        return toDto(saved);
    }

    public void delete(Long id) {
        String tenantId = TenantContext.getCurrent();
        Checkpoint checkpoint = checkpointRepository.findByIdAndTenantId(id, tenantId)
                .orElseThrow(() -> new LocationNotFoundException("Checkpoint not found with id: " + id));
        checkpoint.setActive(false);
        checkpointRepository.save(checkpoint);
    }

    private CheckpointDto toDto(Checkpoint checkpoint) {
        return new CheckpointDto(checkpoint.getId(), checkpoint.getSiteId(),
                checkpoint.getName(), checkpoint.getActive());
    }
}
