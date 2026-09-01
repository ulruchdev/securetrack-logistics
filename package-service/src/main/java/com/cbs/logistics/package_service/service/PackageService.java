package com.cbs.logistics.package_service.service;

import com.cbs.logistics.common.dto.PackageDto;
import com.cbs.logistics.common.dto.PackageStatusChangedEvent;
import com.cbs.logistics.common.security.context.TenantContext;
import com.cbs.logistics.package_service.dto.CreatePackageRequest;
import com.cbs.logistics.package_service.dto.UpdatePackageRequest;
import com.cbs.logistics.package_service.entity.Package;
import com.cbs.logistics.package_service.entity.PackageStatus;
import com.cbs.logistics.package_service.exception.PackageNotFoundException;
import com.cbs.logistics.package_service.mapper.PackageMapper;
import com.cbs.logistics.package_service.repository.PackageRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.Instant;

@Slf4j
@Service
@RequiredArgsConstructor
public class PackageService {
    private final PackageRepository packageRepository;
    private final PackageMapper packageMapper;
    private final EventOutboxService eventOutboxService;

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    public PackageDto create(CreatePackageRequest request) {
        Package entity = packageMapper.toEntity(request);
        entity.setTrackingNumber(generateTrackingNumber());
        entity.setPackageStatus(PackageStatus.NEW);
        entity.setTenantId(TenantContext.getCurrent());
        Package savedEntity = packageRepository.save(entity);

        // Transactional outbox : l'événement est stocké dans la même transaction
        PackageStatusChangedEvent event = new PackageStatusChangedEvent(
                savedEntity.getPackageId(), null, savedEntity.getPackageStatus().name(),
                null, Instant.now()
        );
        eventOutboxService.storePackageStatusChanged(event);

        return packageMapper.toDto(savedEntity);
    }

    public Page<PackageDto> getAll(Pageable page) {
        String tenantId = TenantContext.getCurrent();
        Page<Package> packages = packageRepository.findByTenantIdAndDeletedAtIsNull(tenantId, page);
        return packages.map(packageMapper::toDto);
    }

    public PackageDto update(Long id, UpdatePackageRequest request) {
        String tenantId = TenantContext.getCurrent();
        Package entity = packageRepository.findByPackageIdAndTenantIdAndDeletedAtIsNull(id, tenantId)
                .orElseThrow(() -> new PackageNotFoundException(id));

        PackageStatus previousStatus = entity.getPackageStatus();

        if (request.getPackageStatus() != null) {
            validateStatusTransition(previousStatus, request.getPackageStatus());
        }

        packageMapper.updateEntityFromRequest(request, entity);
        Package updatedEntity = packageRepository.save(entity);

        if (request.getPackageStatus() != null && previousStatus != request.getPackageStatus()) {
            PackageStatusChangedEvent event = new PackageStatusChangedEvent(
                    id, previousStatus.name(), updatedEntity.getPackageStatus().name(),
                    null, Instant.now()
            );
            eventOutboxService.storePackageStatusChanged(event);
        }

        return packageMapper.toDto(updatedEntity);
    }

    public PackageDto getById(Long id) {
        String tenantId = TenantContext.getCurrent();
        Package entity = packageRepository.findByPackageIdAndTenantIdAndDeletedAtIsNull(id, tenantId)
                .orElseThrow(() -> new PackageNotFoundException(id));
        return packageMapper.toDto(entity);
    }

    public PackageDto getByTrackingNumber(String trackingNumber) {
        String tenantId = TenantContext.getCurrent();
        Package entity = packageRepository.findByTrackingNumberAndTenantIdAndDeletedAtIsNull(trackingNumber, tenantId)
                .orElseThrow(() -> new PackageNotFoundException(trackingNumber));
        return packageMapper.toDto(entity);
    }

    /**
     * Soft delete : positionne deleted_at au lieu de supprimer physiquement la ligne.
     * Les requêtes du repository filtrent automatiquement les packages supprimés.
     */
    public void delete(Long id) {
        String tenantId = TenantContext.getCurrent();
        Package entity = packageRepository.findByPackageIdAndTenantIdAndDeletedAtIsNull(id, tenantId)
                .orElseThrow(() -> new PackageNotFoundException(id));
        entity.setDeletedAt(Instant.now());
        packageRepository.save(entity);

        // Événement de suppression dans l'outbox
        PackageStatusChangedEvent event = new PackageStatusChangedEvent(
                id, entity.getPackageStatus().name(), "DELETED",
                null, Instant.now()
        );
        eventOutboxService.storePackageStatusChanged(event);
    }

    /**
     * Génère un tracking number unique au format ST-XXXXXXXX.
     * 8 caractères alphanumériques (A-Z, 0-9) générés de façon cryptographiquement sûre.
     */
    private String generateTrackingNumber() {
        String chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
        StringBuilder sb = new StringBuilder("ST-");
        for (int i = 0; i < 8; i++) {
            sb.append(chars.charAt(SECURE_RANDOM.nextInt(chars.length())));
        }
        return sb.toString();
    }

    private void validateStatusTransition(PackageStatus currentStatus, PackageStatus newStatus) {
        if (currentStatus == newStatus) {
            return;
        }
        if (currentStatus == PackageStatus.DELIVERED && newStatus != PackageStatus.DELIVERED) {
            throw new IllegalArgumentException("Cannot change status from DELIVERED");
        }
        if (currentStatus == PackageStatus.LOST && newStatus != PackageStatus.LOST) {
            throw new IllegalArgumentException("Cannot change status from LOST");
        }
        if (currentStatus == PackageStatus.NEW && newStatus != PackageStatus.IN_TRANSIT && newStatus != PackageStatus.LOST) {
            throw new IllegalArgumentException("From NEW, can only go to IN_TRANSIT or LOST");
        }
        if (currentStatus == PackageStatus.IN_TRANSIT && newStatus != PackageStatus.DELIVERED && newStatus != PackageStatus.LOST) {
            throw new IllegalArgumentException("From IN_TRANSIT, can only go to DELIVERED or LOST");
        }
    }
}
