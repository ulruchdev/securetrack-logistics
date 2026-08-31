package com.cbs.logistics.security_checkpoint_service.service;

import com.cbs.logistics.common.security.context.TenantContext;
import com.cbs.logistics.security_checkpoint_service.client.PackageServiceClient;
import com.cbs.logistics.security_checkpoint_service.dto.CheckpointLogDto;
import com.cbs.logistics.security_checkpoint_service.dto.CreateCheckpointRequest;
import com.cbs.logistics.security_checkpoint_service.entity.CheckpointLog;
import com.cbs.logistics.security_checkpoint_service.exception.CheckpointLogNotFoundException;
import com.cbs.logistics.security_checkpoint_service.exception.CheckpointUnavailableException;
import com.cbs.logistics.security_checkpoint_service.mapper.CheckpointLogMapper;
import com.cbs.logistics.security_checkpoint_service.port.LocationAvailabilityPort;
import com.cbs.logistics.security_checkpoint_service.repository.CheckpointLogRepository;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CheckpointLogService {

    private final CheckpointLogRepository repository;
    private final CheckpointLogMapper mapper;
    private final LocationAvailabilityPort locationAvailabilityPort;
    private final PackageServiceClient packageServiceClient;

    @CircuitBreaker(name = "locationService")
    @Retry(name = "locationService")
    public CheckpointLogDto create(CreateCheckpointRequest request) {

        // 1. Valider que le colis existe par trackingNumber via Package Service
        packageServiceClient.getPackageByTrackingNumber(request.getTrackingNumber());

        // 2. Valider que le checkpoint est actif via Location Service
        LocationAvailabilityPort.CheckpointAvailability checkpoint =
                locationAvailabilityPort.getCheckpointAvailability(request.getCheckpointId());

        if (!checkpoint.active()) {
            throw new CheckpointUnavailableException(
                    "Checkpoint not available: " + request.getCheckpointId());
        }

        // 3. Extraire le createdBy du JWT sub claim
        String createdBy = extractJwtSubject();

        // 4. Construire et sauvegarder
        CheckpointLog entity = mapper.toEntity(request);
        entity.setTenantId(TenantContext.getCurrent());
        entity.setCreatedBy(createdBy);
        CheckpointLog saved = repository.save(entity);
        return mapper.toDto(saved);
    }

    public CheckpointLogDto getById(Long id) {
        CheckpointLog entity = repository.findById(id)
                .orElseThrow(() -> new CheckpointLogNotFoundException(id));
        return mapper.toDto(entity);
    }

    public Page<CheckpointLogDto> getAll(Pageable pageable) {
        String tenantId = TenantContext.getCurrent();
        return repository.findByTenantId(tenantId, pageable).map(mapper::toDto);
    }

    public Page<CheckpointLogDto> getByTrackingNumber(String trackingNumber, Pageable pageable) {
        String tenantId = TenantContext.getCurrent();
        return repository.findByTrackingNumberAndTenantIdOrderByCheckpointTimeDesc(
                trackingNumber, tenantId, pageable).map(mapper::toDto);
    }

    /**
     * Extrait le subject (sub) du JWT depuis le SecurityContext Spring Security.
     */
    private String extractJwtSubject() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getName() != null) {
            return auth.getName();
        }
        return "unknown";
    }
}
