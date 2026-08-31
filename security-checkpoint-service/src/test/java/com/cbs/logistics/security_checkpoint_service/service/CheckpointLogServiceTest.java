package com.cbs.logistics.security_checkpoint_service.service;
import com.cbs.logistics.common.security.context.TenantContext;

import com.cbs.logistics.security_checkpoint_service.client.PackageServiceClient;
import com.cbs.logistics.security_checkpoint_service.dto.CheckpointLogDto;
import com.cbs.logistics.security_checkpoint_service.dto.CreateCheckpointRequest;
import com.cbs.logistics.security_checkpoint_service.entity.CheckpointLog;
import com.cbs.logistics.security_checkpoint_service.entity.CheckpointResult;
import com.cbs.logistics.security_checkpoint_service.exception.CheckpointLogNotFoundException;
import com.cbs.logistics.security_checkpoint_service.exception.CheckpointUnavailableException;
import com.cbs.logistics.security_checkpoint_service.exception.LocationNotFoundException;
import com.cbs.logistics.security_checkpoint_service.mapper.CheckpointLogMapper;
import com.cbs.logistics.security_checkpoint_service.port.LocationAvailabilityPort;
import com.cbs.logistics.security_checkpoint_service.repository.CheckpointLogRepository;
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

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CheckpointLogServiceTest {

    @Mock
    private CheckpointLogRepository repository;

    @Mock
    private CheckpointLogMapper mapper;

    @Mock
    private LocationAvailabilityPort locationAvailabilityPort;

    @Mock
    private PackageServiceClient packageServiceClient;

    @InjectMocks
    private CheckpointLogService service;

    private CheckpointLog entity;
    private CheckpointLogDto dto;
    private CreateCheckpointRequest request;
    private LocationAvailabilityPort.CheckpointAvailability availableCheckpoint;
    private LocationAvailabilityPort.CheckpointAvailability unavailableCheckpoint;

    @BeforeEach
    void setUp() {
        TenantContext.setCurrent("test-tenant");
        LocalDateTime now = LocalDateTime.of(2026, 8, 10, 10, 0);

        entity = CheckpointLog.builder()
                .id(1L)
                .trackingNumber("ST-ABCDEF12")
                .checkpointId(10L)
                .checkpointTime(now)
                .result(CheckpointResult.OK)
                .comment("Passage OK")
                .createdBy("agent-1")
                .build();

        dto = new CheckpointLogDto(1L, "ST-ABCDEF12", 10L, now, CheckpointResult.OK, "Passage OK", "agent-1");

        request = CreateCheckpointRequest.builder()
                .trackingNumber("ST-ABCDEF12")
                .checkpointId(10L)
                .checkpointTime(now)
                .result(CheckpointResult.OK)
                .comment("Passage OK")
                .build();

        // checkpoint active, siteId
        availableCheckpoint = new LocationAvailabilityPort.CheckpointAvailability(true, 1L);
        unavailableCheckpoint = new LocationAvailabilityPort.CheckpointAvailability(false, 1L);
    }

    @Test
    void create_shouldSaveCheckpoint_whenCheckpointAvailable() {
        when(packageServiceClient.getPackageByTrackingNumber("ST-ABCDEF12"))
                .thenReturn(new PackageServiceClient.PackageDto(1L, "ST-ABCDEF12"));
        when(locationAvailabilityPort.getCheckpointAvailability(10L)).thenReturn(availableCheckpoint);
        when(mapper.toEntity(request)).thenReturn(entity);
        when(repository.save(entity)).thenReturn(entity);
        when(mapper.toDto(entity)).thenReturn(dto);

        CheckpointLogDto result = service.create(request);

        assertThat(result).isEqualTo(dto);
        verify(packageServiceClient).getPackageByTrackingNumber("ST-ABCDEF12");
        verify(locationAvailabilityPort).getCheckpointAvailability(10L);
        verify(repository).save(entity);
    }

    @Test
    void create_shouldThrow_whenCheckpointNotAvailable() {
        when(packageServiceClient.getPackageByTrackingNumber("ST-ABCDEF12"))
                .thenReturn(new PackageServiceClient.PackageDto(1L, "ST-ABCDEF12"));
        when(locationAvailabilityPort.getCheckpointAvailability(10L)).thenReturn(unavailableCheckpoint);

        assertThatThrownBy(() -> service.create(request))
                .isInstanceOf(CheckpointUnavailableException.class)
                .hasMessageContaining("10");

        verify(repository, never()).save(any());
    }

    @Test
    void create_shouldPropagate_whenPackageNotFound() {
        when(packageServiceClient.getPackageByTrackingNumber("ST-UNKNOWN"))
                .thenThrow(new LocationNotFoundException("Package not found"));

        CreateCheckpointRequest badRequest = CreateCheckpointRequest.builder()
                .trackingNumber("ST-UNKNOWN")
                .checkpointId(10L)
                .result(CheckpointResult.OK)
                .build();

        assertThatThrownBy(() -> service.create(badRequest))
                .isInstanceOf(LocationNotFoundException.class);

        verify(repository, never()).save(any());
    }

    @Test
    void create_shouldPropagate_whenCheckpointNotFound() {
        when(packageServiceClient.getPackageByTrackingNumber("ST-ABCDEF12"))
                .thenReturn(new PackageServiceClient.PackageDto(1L, "ST-ABCDEF12"));
        when(locationAvailabilityPort.getCheckpointAvailability(99L))
                .thenThrow(new LocationNotFoundException("Checkpoint not found"));

        CreateCheckpointRequest badRequest = CreateCheckpointRequest.builder()
                .trackingNumber("ST-ABCDEF12")
                .checkpointId(99L)
                .result(CheckpointResult.OK)
                .build();

        assertThatThrownBy(() -> service.create(badRequest))
                .isInstanceOf(LocationNotFoundException.class);

        verify(repository, never()).save(any());
    }

    @Test
    void getById_shouldReturnCheckpoint() {
        when(repository.findById(1L)).thenReturn(Optional.of(entity));
        when(mapper.toDto(entity)).thenReturn(dto);

        CheckpointLogDto result = service.getById(1L);

        assertThat(result).isEqualTo(dto);
    }

    @Test
    void getById_shouldThrow_whenNotFound() {
        when(repository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getById(99L))
                .isInstanceOf(CheckpointLogNotFoundException.class)
                .hasMessage("Checkpoint log not found with id: 99");
    }

    @Test
    void getAll_shouldReturnPagedCheckpoints() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<CheckpointLog> page = new PageImpl<>(List.of(entity), pageable, 1);
        when(repository.findByTenantId("test-tenant", pageable)).thenReturn(page);
        when(mapper.toDto(entity)).thenReturn(dto);

        Page<CheckpointLogDto> result = service.getAll(pageable);

        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0)).isEqualTo(dto);
    }

    @Test
    void getByTrackingNumber_shouldReturnPagedCheckpoints() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<CheckpointLog> page = new PageImpl<>(List.of(entity), pageable, 1);
        when(repository.findByTrackingNumberAndTenantIdOrderByCheckpointTimeDesc(
                "ST-ABCDEF12", "test-tenant", pageable)).thenReturn(page);
        when(mapper.toDto(entity)).thenReturn(dto);

        Page<CheckpointLogDto> result = service.getByTrackingNumber("ST-ABCDEF12", pageable);

        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0)).isEqualTo(dto);
        verify(repository).findByTrackingNumberAndTenantIdOrderByCheckpointTimeDesc(
                "ST-ABCDEF12", "test-tenant", pageable);
    }
}
