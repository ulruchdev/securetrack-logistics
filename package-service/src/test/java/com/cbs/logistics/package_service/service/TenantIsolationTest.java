package com.cbs.logistics.package_service.service;

import com.cbs.logistics.common.dto.PackageDto;
import com.cbs.logistics.common.security.context.TenantContext;
import com.cbs.logistics.package_service.dto.CreatePackageRequest;
import com.cbs.logistics.package_service.dto.UpdatePackageRequest;
import com.cbs.logistics.package_service.entity.Package;
import com.cbs.logistics.package_service.entity.PackageStatus;
import com.cbs.logistics.package_service.exception.PackageNotFoundException;
import com.cbs.logistics.package_service.mapper.PackageMapper;
import com.cbs.logistics.package_service.repository.PackageRepository;
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
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TenantIsolationTest {

    private static final String TENANT_A = "tenant-alpha";
    private static final String TENANT_B = "tenant-beta";

    @Mock private PackageRepository packageRepository;
    @Mock private PackageMapper packageMapper;
    @Mock private EventOutboxService eventOutboxService;
    @InjectMocks private PackageService packageService;

    private Package entityA;
    private PackageDto dtoA;

    @BeforeEach
    void setUp() {
        entityA = new Package();
        entityA.setPackageId(1L);
        entityA.setTenantId(TENANT_A);
        entityA.setDescription("Package belonging to Tenant A");
        entityA.setPackageStatus(PackageStatus.NEW);
        dtoA = new PackageDto(1L, "ST-ABCDEF12", "Package belonging to Tenant A", null, null, 2.5, false, "NEW");
    }

    @AfterEach
    void tearDown() { TenantContext.clear(); }

    @Test
    void create_shouldSetTenantId_fromJwt() {
        TenantContext.setCurrent(TENANT_A);
        CreatePackageRequest request = new CreatePackageRequest();
        request.setDescription("New package");
        when(packageMapper.toEntity(request)).thenReturn(entityA);
        when(packageRepository.save(any())).thenReturn(entityA);
        when(packageMapper.toDto(any())).thenReturn(dtoA);
        packageService.create(request);
        assertThat(entityA.getTenantId()).isEqualTo(TENANT_A);
    }

    @Test
    void getById_shouldReturn404_whenAccessingOtherTenantData() {
        TenantContext.setCurrent(TENANT_B);
        when(packageRepository.findByPackageIdAndTenantIdAndDeletedAtIsNull(1L, TENANT_B)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> packageService.getById(1L)).isInstanceOf(PackageNotFoundException.class);
    }

    @Test
    void update_shouldFail_whenAccessingOtherTenantData() {
        TenantContext.setCurrent(TENANT_B);
        when(packageRepository.findByPackageIdAndTenantIdAndDeletedAtIsNull(1L, TENANT_B)).thenReturn(Optional.empty());
        UpdatePackageRequest request = new UpdatePackageRequest();
        request.setPackageStatus(PackageStatus.IN_TRANSIT);
        assertThatThrownBy(() -> packageService.update(1L, request)).isInstanceOf(PackageNotFoundException.class);
    }

    @Test
    void delete_shouldFail_whenAccessingOtherTenantData() {
        TenantContext.setCurrent(TENANT_B);
        when(packageRepository.findByPackageIdAndTenantIdAndDeletedAtIsNull(1L, TENANT_B)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> packageService.delete(1L)).isInstanceOf(PackageNotFoundException.class);
    }

    @Test
    void getAll_shouldOnlyReturnOwnTenantData() {
        TenantContext.setCurrent(TENANT_A);
        Pageable pageable = PageRequest.of(0, 10);
        Page<Package> page = new PageImpl<>(List.of(entityA), pageable, 1);
        when(packageRepository.findByTenantIdAndDeletedAtIsNull(TENANT_A, pageable)).thenReturn(page);
        when(packageMapper.toDto(entityA)).thenReturn(dtoA);
        var result = packageService.getAll(pageable);
        assertThat(result.getContent()).hasSize(1);
        verify(packageRepository).findByTenantIdAndDeletedAtIsNull(TENANT_A, pageable);
    }

    @Test
    void getAll_shouldReturnEmpty_whenOtherTenantHasData() {
        TenantContext.setCurrent(TENANT_B);
        Pageable pageable = PageRequest.of(0, 10);
        Page<Package> emptyPage = new PageImpl<>(List.of(), pageable, 0);
        when(packageRepository.findByTenantIdAndDeletedAtIsNull(TENANT_B, pageable)).thenReturn(emptyPage);
        var result = packageService.getAll(pageable);
        assertThat(result.getContent()).isEmpty();
    }
}
