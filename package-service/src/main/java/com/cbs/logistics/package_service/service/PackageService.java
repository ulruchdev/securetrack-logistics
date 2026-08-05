package com.cbs.logistics.package_service.service;

import com.cbs.logistics.package_service.dto.CreatePackageRequest;
import com.cbs.logistics.package_service.dto.PackageDto;
import com.cbs.logistics.package_service.dto.UpdatePackageRequest;
import com.cbs.logistics.package_service.entity.Package;
import com.cbs.logistics.package_service.entity.PackageStatus;
import com.cbs.logistics.package_service.exception.PackageNotFoundException;
import com.cbs.logistics.package_service.mapper.PackageMapper;
import com.cbs.logistics.package_service.repository.PackageRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class PackageService {
    private final PackageRepository packageRepository;
    private final PackageMapper packageMapper;

    public PackageDto create(CreatePackageRequest request) {
        if (request.getDescription() == null || request.getDescription().trim().isEmpty()) {
            throw new IllegalArgumentException("Description cannot be null or empty");
        }
        Package entity = packageMapper.toEntity(request);
        entity.setPackageStatus(PackageStatus.NEW);
        Package savedEntity = packageRepository.save(entity);
        return packageMapper.toDto(savedEntity);
    }



    public Page<PackageDto> getAll(Pageable page){
        Page<Package> packages=packageRepository.findAll(page);
        return packages.map(packageMapper::toDto);
    }

    public PackageDto update(Long id, UpdatePackageRequest request) {
        Package entity = packageRepository.findById(id)
                .orElseThrow(() -> new PackageNotFoundException(id));


        if (request.getPackageStatus() != null) {
            validateStatusTransition(entity.getPackageStatus(), request.getPackageStatus());
        }

        packageMapper.updateEntityFromRequest(request, entity);
        Package updatedEntity = packageRepository.save(entity);
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


}
