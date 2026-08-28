package com.cbs.logistics.package_service.service;

import com.cbs.logistics.common.dto.PackageDto;
import com.cbs.logistics.common.dto.PackageStatusChangedEvent;
import com.cbs.logistics.package_service.dto.CreatePackageRequest;
import com.cbs.logistics.package_service.dto.UpdatePackageRequest;
import com.cbs.logistics.package_service.entity.Package;
import com.cbs.logistics.package_service.entity.PackageStatus;
import com.cbs.logistics.package_service.exception.PackageNotFoundException;
import com.cbs.logistics.package_service.mapper.PackageMapper;
import com.cbs.logistics.package_service.repository.PackageRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.time.Instant;

@Slf4j
@Service
@RequiredArgsConstructor
public class PackageService {
    private final PackageRepository packageRepository;
    private final PackageMapper packageMapper;
    private final RabbitTemplate rabbitTemplate;

    public PackageDto create(CreatePackageRequest request) {
        // La validation des champs est assurée par Bean Validation (@Valid au niveau du controller)
        Package entity = packageMapper.toEntity(request);
        entity.setPackageStatus(PackageStatus.NEW);
        Package savedEntity = packageRepository.save(entity);

        // Publication de l'evenement : le Tracking Service sera notifie automatiquement
        publishStatusChanged(savedEntity.getPackageId(), null, savedEntity.getPackageStatus());

        return packageMapper.toDto(savedEntity);
    }



    public Page<PackageDto> getAll(Pageable page){
        Page<Package> packages=packageRepository.findAll(page);
        return packages.map(packageMapper::toDto);
    }

    public PackageDto update(Long id, UpdatePackageRequest request) {
        Package entity = packageRepository.findById(id)
                .orElseThrow(() -> new PackageNotFoundException(id));

        PackageStatus previousStatus = entity.getPackageStatus();

        if (request.getPackageStatus() != null) {
            validateStatusTransition(previousStatus, request.getPackageStatus());
        }

        packageMapper.updateEntityFromRequest(request, entity);
        Package updatedEntity = packageRepository.save(entity);

        // Publication de l'événement si le statut a changé
        if (request.getPackageStatus() != null && previousStatus != request.getPackageStatus()) {
            publishStatusChanged(id, previousStatus, updatedEntity.getPackageStatus());
        }

        return packageMapper.toDto(updatedEntity);
    }

    public PackageDto getById(Long id) {
        Package entity = packageRepository.findById(id)
                .orElseThrow(() -> new PackageNotFoundException(id));
        return packageMapper.toDto(entity);
    }

    public void delete(Long id) {
        if (!packageRepository.existsById(id)) {
            throw new PackageNotFoundException(id);
        }
        packageRepository.deleteById(id);
    }

    private void validateStatusTransition(PackageStatus currentStatus, PackageStatus newStatus) {
        // Conserver le même statut est toujours permis (PATCH partiel sans changement de statut)
        if (currentStatus == newStatus) {
            return;
        }
        if (currentStatus == PackageStatus.DELIVERED && newStatus != PackageStatus.DELIVERED) {
            throw new IllegalArgumentException("Cannot change status from DELIVERED");
        }
        if (currentStatus == PackageStatus.LOST && newStatus != PackageStatus.LOST) {
            throw new IllegalArgumentException("Cannot change status from LOST");
        }
        // Allow transitions: NEW -> IN_TRANSIT -> DELIVERED or LOST
        if (currentStatus == PackageStatus.NEW && newStatus != PackageStatus.IN_TRANSIT && newStatus != PackageStatus.LOST) {
            throw new IllegalArgumentException("From NEW, can only go to IN_TRANSIT or LOST");
        }
        if (currentStatus == PackageStatus.IN_TRANSIT && newStatus != PackageStatus.DELIVERED && newStatus != PackageStatus.LOST) {
            throw new IllegalArgumentException("From IN_TRANSIT, can only go to DELIVERED or LOST");
        }
    }

    private void publishStatusChanged(Long packageId, PackageStatus previousStatus,
                                      PackageStatus newStatus) {
        try {
            PackageStatusChangedEvent event = new PackageStatusChangedEvent(
                    packageId,
                    previousStatus != null ? previousStatus.name() : null,
                    newStatus.name(),
                    null, // locationId non disponible côté Package Service
                    Instant.now()
            );
            rabbitTemplate.convertAndSend("package-status", "status.changed", event);
            log.info("Event published: package {} status {} -> {}", packageId, previousStatus, newStatus);
        } catch (Exception e) {
            // L'échec de publication ne doit pas faire échouer l'update
            // Le Tracking Service peut aussi être appelé manuellement
            log.warn("Failed to publish status changed event for package {}: {}", packageId, e.getMessage());
        }
    }
}
