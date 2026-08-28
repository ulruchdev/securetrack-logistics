package com.cbs.logistics.package_service.service;

import com.cbs.logistics.common.dto.PackageStatusChangedEvent;
import com.cbs.logistics.package_service.dto.UpdatePackageRequest;
import com.cbs.logistics.package_service.entity.Package;
import com.cbs.logistics.package_service.entity.PackageStatus;
import com.cbs.logistics.package_service.mapper.PackageMapper;
import com.cbs.logistics.package_service.repository.PackageRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PackageServiceEventPublishTest {

    @Mock
    private PackageRepository packageRepository;

    @Mock
    private PackageMapper packageMapper;

    @Mock
    private RabbitTemplate rabbitTemplate;

    @InjectMocks
    private PackageService packageService;

    private Package packageEntity;

    @BeforeEach
    void setUp() {
        packageEntity = new Package();
        packageEntity.setPackageId(1L);
        packageEntity.setPackageStatus(PackageStatus.NEW);
    }

    @Test
    void update_ShouldPublishEvent_WhenStatusChanges() {
        // Given
        UpdatePackageRequest request = new UpdatePackageRequest();
        request.setPackageStatus(PackageStatus.IN_TRANSIT);

        when(packageRepository.findById(1L)).thenReturn(Optional.of(packageEntity));
        doAnswer(invocation -> {
            UpdatePackageRequest req = invocation.getArgument(0);
            Package entity = invocation.getArgument(1);
            entity.setPackageStatus(req.getPackageStatus());
            return null;
        }).when(packageMapper).updateEntityFromRequest(any(), any());
        when(packageRepository.save(any())).thenReturn(packageEntity);

        // When
        packageService.update(1L, request);

        // Then
        ArgumentCaptor<PackageStatusChangedEvent> captor = ArgumentCaptor.forClass(PackageStatusChangedEvent.class);
        verify(rabbitTemplate).convertAndSend(eq("package-status"), eq("status.changed"), captor.capture());

        PackageStatusChangedEvent event = captor.getValue();
        assertThat(event.packageId()).isEqualTo(1L);
        assertThat(event.previousStatus()).isEqualTo("NEW");
        assertThat(event.newStatus()).isEqualTo("IN_TRANSIT");
        assertThat(event.locationId()).isNull();
        assertThat(event.timestamp()).isNotNull();
    }

    @Test
    void update_ShouldNotPublishEvent_WhenStatusUnchanged() {
        // Given : même statut = pas de changement
        packageEntity.setPackageStatus(PackageStatus.IN_TRANSIT);
        UpdatePackageRequest request = new UpdatePackageRequest();
        request.setPackageStatus(PackageStatus.IN_TRANSIT);

        when(packageRepository.findById(1L)).thenReturn(Optional.of(packageEntity));
        when(packageRepository.save(any())).thenReturn(packageEntity);

        // When
        packageService.update(1L, request);

        // Then : aucun event publié
        verify(rabbitTemplate, never()).convertAndSend(anyString(), anyString(), any(Object.class));
    }

    @Test
    void update_ShouldNotPublishEvent_WhenStatusIsNull() {
        // Given : PATCH sans changement de statut
        UpdatePackageRequest request = new UpdatePackageRequest();
        request.setPackageStatus(null);

        when(packageRepository.findById(1L)).thenReturn(Optional.of(packageEntity));
        when(packageRepository.save(any())).thenReturn(packageEntity);

        // When
        packageService.update(1L, request);

        // Then : aucun event publié
        verify(rabbitTemplate, never()).convertAndSend(anyString(), anyString(), any(Object.class));
    }
}
