package com.securetrack.packageservice.application.service;

import com.securetrack.packageservice.application.dto.command.CreatePackageCommand;
import com.securetrack.packageservice.application.dto.result.PackageResult;
import com.securetrack.packageservice.application.port.out.PackageRepositoryPort;
import com.securetrack.packageservice.domain.model.Package;
import com.securetrack.packageservice.domain.Enum.PackageStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CreatePackageServiceTest {

    @Mock
    private PackageRepositoryPort packageRepositoryPort;

    @InjectMocks
    private CreatePackageService createPackageService;

    private CreatePackageCommand validCommand;

    @BeforeEach
    void setUp() {
        validCommand = CreatePackageCommand.builder()
                .description("Colis médical urgent")
                .weight(12.5)
                .fragile(true)
                .build();
    }

    @Test
    void createPackage_shouldReturnSuccess_whenValidCommand() {
        // Given
        Package savedDomain = Package.create("Colis médical urgent", 12.5, true);
        savedDomain.setPackageId("550e8400-e29b-41d4-a716-446655440000");

        when(packageRepositoryPort.save(any(Package.class))).thenReturn(savedDomain);

        // When
        PackageResult result = createPackageService.createPackage(validCommand);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.description()).isEqualTo("Colis médical urgent");
        assertThat(result.weight()).isEqualTo(12.5);
        assertThat(result.status()).isEqualTo(PackageStatus.CREATED);
        assertThat(result.packageId()).isNotBlank();
    }

    @Test
    void createPackage_shouldThrowException_whenWeightInvalid() {
        // Given
        CreatePackageCommand invalidCommand = CreatePackageCommand.builder()
                .description("Colis test")
                .weight(-5.0)        // poids invalide
                .fragile(false)
                .build();

        // When & Then
        org.junit.jupiter.api.Assertions.assertThrows(
                IllegalArgumentException.class,
                () -> createPackageService.createPackage(invalidCommand)
        );
    }
}