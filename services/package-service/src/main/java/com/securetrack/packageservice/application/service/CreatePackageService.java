package com.securetrack.packageservice.application.service;

import com.securetrack.packageservice.application.dto.command.CreatePackageCommand;
import com.securetrack.packageservice.application.dto.result.PackageResult;
import com.securetrack.packageservice.application.port.in.CreatePackageUseCase;
import com.securetrack.packageservice.application.port.out.PackageRepositoryPort;
import com.securetrack.packageservice.domain.model.Package;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class CreatePackageService implements CreatePackageUseCase {

    private final PackageRepositoryPort packageRepositoryPort;

    @Override
    public PackageResult createPackage(CreatePackageCommand command) {
        // Validate weight
        if (command.weight() <= 0 || command.weight() > 500) {
            throw new IllegalArgumentException("Weight must be between 1 and 500");
        }
        
        Package packageDomain = Package.create(
                command.description(),
                command.weight(),
                command.fragile()
        );

        Package saved = packageRepositoryPort.save(packageDomain);
        return PackageResult.builder()
                .packageId(saved.getPackageId())
                .description(saved.getDescription())
                .weight(saved.getWeight())
                .fragile(saved.isFragile())
                .status(saved.getStatus())
                .createdAt(saved.getCreatedAt())
                .updatedAt(saved.getUpdatedAt())
                .build();
    }
}