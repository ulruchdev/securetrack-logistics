package com.cbs.logistics.package_service.service;

import com.cbs.logistics.common.security.context.TenantContext;
import com.cbs.logistics.package_service.dto.CreatePackageRequest;
import com.cbs.logistics.common.dto.PackageDto;
import com.cbs.logistics.package_service.dto.UpdatePackageRequest;
import com.cbs.logistics.package_service.entity.Package;
import com.cbs.logistics.package_service.entity.PackageStatus;
import com.cbs.logistics.package_service.exception.PackageNotFoundException;
import com.cbs.logistics.package_service.mapper.PackageMapper;
import com.cbs.logistics.package_service.repository.PackageRepository;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.AfterEach;
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
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PackageServiceTest {

    private static final String TENANT_ID = "test-tenant";

    @Mock
    private PackageRepository packageRepository;

    @Mock
    private PackageMapper packageMapper;

    @InjectMocks
    private PackageService packageService;

    private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

    private Package packageEntity;
    private PackageDto packageDto;
    private CreatePackageRequest createRequest;
    private UpdatePackageRequest updateRequest;

    @BeforeEach
    void setUp() {
        TenantContext.setCurrent(TENANT_ID);

        packageEntity = new Package();
        packageEntity.setPackageId(1L);
        packageEntity.setTenantId(TENANT_ID);
        packageEntity.setDescription("Test Package");
        packageEntity.setWeight(2.5);
        packageEntity.setFragile(true);
        packageEntity.setPackageStatus(PackageStatus.NEW);

        packageDto = new PackageDto(1L, "Test Package", null, null, 2.5, true, "NEW");

        createRequest = new CreatePackageRequest();
        createRequest.setDescription("Test Package");
        createRequest.setWeight(2.5);
        createRequest.setFragile(true);

        updateRequest = new UpdatePackageRequest();
        updateRequest.setPackageStatus(PackageStatus.IN_TRANSIT);
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }

    @Test
    void create_ShouldCreatePackageWithDefaultStatus() {
        when(packageMapper.toEntity(createRequest)).thenReturn(packageEntity);
        when(packageRepository.save(packageEntity)).thenReturn(packageEntity);
        when(packageMapper.toDto(packageEntity)).thenReturn(packageDto);

        PackageDto result = packageService.create(createRequest);

        assertThat(result).isEqualTo(packageDto);
        assertThat(packageEntity.getPackageStatus()).isEqualTo(PackageStatus.NEW);
        verify(packageRepository).save(packageEntity);
    }

    @Test
    void createRequest_ShouldRejectBlankDescription_WhenValidated() {
        createRequest.setDescription("");
        var violations = validator.validate(createRequest);
        assertThat(violations).isNotEmpty();
    }

    @Test
    void update_ShouldUpdatePackage() {
        when(packageRepository.findByPackageIdAndTenantId(1L, TENANT_ID)).thenReturn(Optional.of(packageEntity));
        when(packageRepository.save(packageEntity)).thenReturn(packageEntity);
        when(packageMapper.toDto(packageEntity)).thenReturn(packageDto);

        doAnswer(invocation -> {
            UpdatePackageRequest req = invocation.getArgument(0);
            Package entity = invocation.getArgument(1);
            if (req.getPackageStatus() != null) {
                entity.setPackageStatus(req.getPackageStatus());
            }
            return null;
        }).when(packageMapper).updateEntityFromRequest(any(UpdatePackageRequest.class), any(Package.class));

        PackageDto result = packageService.update(1L, updateRequest);

        assertThat(result).isEqualTo(packageDto);
        assertThat(packageEntity.getPackageStatus()).isEqualTo(PackageStatus.IN_TRANSIT);
    }

    @Test
    void update_ShouldThrowException_WhenPackageNotFound() {
        when(packageRepository.findByPackageIdAndTenantId(1L, TENANT_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> packageService.update(1L, updateRequest))
                .isInstanceOf(PackageNotFoundException.class);
    }

    @Test
    void update_ShouldThrowException_WhenInvalidStatusTransition() {
        packageEntity.setPackageStatus(PackageStatus.DELIVERED);
        updateRequest.setPackageStatus(PackageStatus.NEW);
        when(packageRepository.findByPackageIdAndTenantId(1L, TENANT_ID)).thenReturn(Optional.of(packageEntity));

        assertThatThrownBy(() -> packageService.update(1L, updateRequest))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Cannot change status from DELIVERED");
    }

    @Test
    void update_ShouldAllowSameStatus() {
        packageEntity.setPackageStatus(PackageStatus.IN_TRANSIT);
        updateRequest.setPackageStatus(PackageStatus.IN_TRANSIT);
        when(packageRepository.findByPackageIdAndTenantId(1L, TENANT_ID)).thenReturn(Optional.of(packageEntity));
        when(packageRepository.save(packageEntity)).thenReturn(packageEntity);
        when(packageMapper.toDto(packageEntity)).thenReturn(packageDto);

        PackageDto result = packageService.update(1L, updateRequest);

        assertThat(result).isEqualTo(packageDto);
        verify(packageRepository).save(packageEntity);
    }

    @Test
    void update_ShouldAllowTransitionFromNewToInTransit() {
        packageEntity.setPackageStatus(PackageStatus.NEW);
        updateRequest.setPackageStatus(PackageStatus.IN_TRANSIT);
        when(packageRepository.findByPackageIdAndTenantId(1L, TENANT_ID)).thenReturn(Optional.of(packageEntity));
        when(packageRepository.save(packageEntity)).thenReturn(packageEntity);
        when(packageMapper.toDto(packageEntity)).thenReturn(packageDto);

        PackageDto result = packageService.update(1L, updateRequest);

        assertThat(result).isEqualTo(packageDto);
        verify(packageRepository).save(packageEntity);
    }

    @Test
    void getById_ShouldReturnPackage() {
        when(packageRepository.findByPackageIdAndTenantId(1L, TENANT_ID)).thenReturn(Optional.of(packageEntity));
        when(packageMapper.toDto(packageEntity)).thenReturn(packageDto);

        PackageDto result = packageService.getById(1L);

        assertThat(result).isEqualTo(packageDto);
    }

    @Test
    void getById_ShouldThrowException_WhenPackageNotFound() {
        when(packageRepository.findByPackageIdAndTenantId(1L, TENANT_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> packageService.getById(1L))
                .isInstanceOf(PackageNotFoundException.class);
    }

    @Test
    void delete_ShouldDeletePackage() {
        when(packageRepository.existsByPackageIdAndTenantId(1L, TENANT_ID)).thenReturn(true);

        packageService.delete(1L);

        verify(packageRepository).deleteById(1L);
    }

    @Test
    void delete_ShouldThrowException_WhenPackageNotFound() {
        when(packageRepository.existsByPackageIdAndTenantId(1L, TENANT_ID)).thenReturn(false);

        assertThatThrownBy(() -> packageService.delete(1L))
                .isInstanceOf(PackageNotFoundException.class);
    }

    @Test
    void getAll_ShouldReturnPagedPackages() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<Package> packagePage = new PageImpl<>(List.of(packageEntity), pageable, 1);
        when(packageRepository.findByTenantId(TENANT_ID, pageable)).thenReturn(packagePage);
        when(packageMapper.toDto(packageEntity)).thenReturn(packageDto);

        Page<PackageDto> result = packageService.getAll(pageable);

        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0)).isEqualTo(packageDto);
    }
}
