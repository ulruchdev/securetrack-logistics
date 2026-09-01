package com.cbs.logistics.package_service.service;

import com.cbs.logistics.common.dto.PackageStatusChangedEvent;
import com.cbs.logistics.common.security.context.TenantContext;
import com.cbs.logistics.package_service.dto.UpdatePackageRequest;
import com.cbs.logistics.package_service.entity.Package;
import com.cbs.logistics.package_service.entity.PackageStatus;
import com.cbs.logistics.package_service.mapper.PackageMapper;
import com.cbs.logistics.package_service.repository.PackageRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PackageServiceEventPublishTest {

    private static final String TENANT_ID = "test-tenant";

    @Mock
    private PackageRepository packageRepository;

    @Mock
    private PackageMapper packageMapper;

    @Mock
    private EventOutboxService eventOutboxService;

    @InjectMocks
    private PackageService packageService;

    private Package packageEntity;

    @BeforeEach
    void setUp() {
        TenantContext.setCurrent(TENANT_ID);
        packageEntity = new Package();
        packageEntity.setPackageId(1L);
        packageEntity.setTenantId(TENANT_ID);
        packageEntity.setPackageStatus(PackageStatus.NEW);
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }

    @Test
    void update_ShouldStoreEventInOutbox_WhenStatusChanges() {
        UpdatePackageRequest request = new UpdatePackageRequest();
        request.setPackageStatus(PackageStatus.IN_TRANSIT);

        when(packageRepository.findByPackageIdAndTenantIdAndDeletedAtIsNull(1L, TENANT_ID)).thenReturn(Optional.of(packageEntity));
        doAnswer(invocation -> {
            UpdatePackageRequest req = invocation.getArgument(0);
            Package entity = invocation.getArgument(1);
            entity.setPackageStatus(req.getPackageStatus());
            return null;
        }).when(packageMapper).updateEntityFromRequest(any(), any());
        when(packageRepository.save(any())).thenReturn(packageEntity);

        packageService.update(1L, request);

        ArgumentCaptor<PackageStatusChangedEvent> captor = ArgumentCaptor.forClass(PackageStatusChangedEvent.class);
        verify(eventOutboxService).storePackageStatusChanged(captor.capture());

        PackageStatusChangedEvent event = captor.getValue();
        assertThat(event.packageId()).isEqualTo(1L);
        assertThat(event.previousStatus()).isEqualTo("NEW");
        assertThat(event.newStatus()).isEqualTo("IN_TRANSIT");
    }

    @Test
    void update_ShouldNotStoreEvent_WhenStatusUnchanged() {
        packageEntity.setPackageStatus(PackageStatus.IN_TRANSIT);
        UpdatePackageRequest request = new UpdatePackageRequest();
        request.setPackageStatus(PackageStatus.IN_TRANSIT);

        when(packageRepository.findByPackageIdAndTenantIdAndDeletedAtIsNull(1L, TENANT_ID)).thenReturn(Optional.of(packageEntity));
        when(packageRepository.save(any())).thenReturn(packageEntity);

        packageService.update(1L, request);

        verify(eventOutboxService, never()).storePackageStatusChanged(any());
    }

    @Test
    void update_ShouldNotStoreEvent_WhenStatusIsNull() {
        UpdatePackageRequest request = new UpdatePackageRequest();
        request.setPackageStatus(null);

        when(packageRepository.findByPackageIdAndTenantIdAndDeletedAtIsNull(1L, TENANT_ID)).thenReturn(Optional.of(packageEntity));
        when(packageRepository.save(any())).thenReturn(packageEntity);

        packageService.update(1L, request);

        verify(eventOutboxService, never()).storePackageStatusChanged(any());
    }
}
