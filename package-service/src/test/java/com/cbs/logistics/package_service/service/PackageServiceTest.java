package com.cbs.logistics.package_service.service;

import com.cbs.logistics.package_service.dto.CreatePackageRequest;
import com.cbs.logistics.package_service.dto.PackageDto;
import com.cbs.logistics.package_service.dto.UpdatePackageRequest;
import com.cbs.logistics.package_service.entity.Package;
import com.cbs.logistics.package_service.entity.PackageStatus;
import com.cbs.logistics.package_service.exception.PackageNotFoundException;
import com.cbs.logistics.package_service.mapper.PackageMapper;
import com.cbs.logistics.package_service.repository.PackageRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PackageServiceTest {

    @Mock
    private PackageRepository packageRepository;

    @Mock
    private PackageMapper packageMapper;

    @InjectMocks
    private PackageService packageService;

    private Package packageEntity;
    private PackageDto packageDto;
    private CreatePackageRequest createRequest;
    private UpdatePackageRequest updateRequest;

    @BeforeEach
    void setUp() {
        packageEntity = new Package();
        packageEntity.setPackageId(1L);
        packageEntity.setDescription("Test Package");
        packageEntity.setWeight(2.5);
        packageEntity.setFragile(true);
        packageEntity.setPackageStatus(PackageStatus.NEW);

        packageDto = new PackageDto(1L, "Test Package", null, null, 2.5, true, PackageStatus.NEW);

        createRequest = new CreatePackageRequest();
        createRequest.setDescription("Test Package");
        createRequest.setWeight(2.5);
        createRequest.setFragile(true);

        updateRequest = new UpdatePackageRequest();
        updateRequest.setPackageStatus(PackageStatus.IN_TRANSIT);
    }

    @Test
    void create_ShouldCreatePackageWithDefaultStatus() {
        // Given
        when(packageMapper.toEntity(createRequest)).thenReturn(packageEntity);
        when(packageRepository.save(packageEntity)).thenReturn(packageEntity);
        when(packageMapper.toDto(packageEntity)).thenReturn(packageDto);

        // When
        PackageDto result = packageService.create(createRequest);

        // Then
        assertThat(result).isEqualTo(packageDto);
        assertThat(packageEntity.getPackageStatus()).isEqualTo(PackageStatus.NEW);
        verify(packageRepository).save(packageEntity);
    }

    @Test
    void create_ShouldThrowException_WhenDescriptionIsInvalid() {
        // Given
        createRequest.setDescription("");

        // When & Then
        assertThatThrownBy(() -> packageService.create(createRequest))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Description cannot be null or empty");
    }

    @Test
    void update_ShouldUpdatePackage() {
        // Given
        when(packageRepository.findById(1L)).thenReturn(Optional.of(packageEntity));
        when(packageRepository.save(packageEntity)).thenReturn(packageEntity);
        when(packageMapper.toDto(packageEntity)).thenReturn(packageDto);

        // When
        PackageDto result = packageService.update(1L, updateRequest);

        // Then
        assertThat(result).isEqualTo(packageDto);
        assertThat(packageEntity.getPackageStatus()).isEqualTo(PackageStatus.IN_TRANSIT);
        verify(packageMapper).updateEntityFromRequest(updateRequest, packageEntity);
    }

    @Test
    void update_ShouldThrowException_WhenPackageNotFound() {
        // Given
        when(packageRepository.findById(1L)).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> packageService.update(1L, updateRequest))
                .isInstanceOf(PackageNotFoundException.class)
                .hasMessage("Package not found with id: 1");
    }

    @Test
    void update_ShouldThrowException_WhenInvalidStatusTransition() {
        // Given
        packageEntity.setPackageStatus(PackageStatus.DELIVERED);
        updateRequest.setPackageStatus(PackageStatus.NEW);
        when(packageRepository.findById(1L)).thenReturn(Optional.of(packageEntity));

        // When & Then
        assertThatThrownBy(() -> packageService.update(1L, updateRequest))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Cannot change status from DELIVERED");
    }

    @Test
    void getById_ShouldReturnPackage() {
        // Given
        when(packageRepository.findById(1L)).thenReturn(Optional.of(packageEntity));
        when(packageMapper.toDto(packageEntity)).thenReturn(packageDto);

        // When
        PackageDto result = packageService.getById(1L);

        // Then
        assertThat(result).isEqualTo(packageDto);
    }

    @Test
    void getById_ShouldThrowException_WhenPackageNotFound() {
        // Given
        when(packageRepository.findById(1L)).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> packageService.getById(1L))
                .isInstanceOf(PackageNotFoundException.class)
                .hasMessage("Package not found with id: 1");
    }

    @Test
    void delete_ShouldDeletePackage() {
        // Given
        when(packageRepository.existsById(1L)).thenReturn(true);

        // When
        packageService.delete(1L);

        // Then
        verify(packageRepository).deleteById(1L);
    }

    @Test
    void delete_ShouldThrowException_WhenPackageNotFound() {
        // Given
        when(packageRepository.existsById(1L)).thenReturn(false);

        // When & Then
        assertThatThrownBy(() -> packageService.delete(1L))
                .isInstanceOf(PackageNotFoundException.class)
                .hasMessage("Package not found with id: 1");
    }

    @Test
    void getAll_ShouldReturnPagedPackages() {
        // Given
        Pageable pageable = PageRequest.of(0, 10);
        Page<Package> packagePage = new PageImpl<>(List.of(packageEntity), pageable, 1);
        when(packageRepository.findAll(pageable)).thenReturn(packagePage);
        when(packageMapper.toDto(packageEntity)).thenReturn(packageDto);

        // When
        Page<PackageDto> result = packageService.getAll(pageable);

        // Then
        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0)).isEqualTo(packageDto);
    }
}