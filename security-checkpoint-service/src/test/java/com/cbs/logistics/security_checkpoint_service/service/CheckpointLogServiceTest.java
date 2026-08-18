package com.cbs.logistics.security_checkpoint_service.service;

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

    @InjectMocks
    private CheckpointLogService service;

    private CheckpointLog entity;
    private CheckpointLogDto dto;
    private CreateCheckpointRequest request;
    private LocationAvailabilityPort.LocationAvailability availableLocation;
    private LocationAvailabilityPort.LocationAvailability unavailableLocation;

    @BeforeEach
    void setUp() {
        LocalDateTime now = LocalDateTime.of(2026, 8, 10, 10, 0);

        entity = CheckpointLog.builder()
                .id(1L)
                .packageId(1L)
                .locationId("loc-1")
                .checkpointTime(now)
                .result(CheckpointResult.OK)
                .comment("Passage OK")
                .createdBy("agent-1")
                .build();

        dto = new CheckpointLogDto(1L, 1L, "loc-1", now, CheckpointResult.OK, "Passage OK", "agent-1");

        request = new CreateCheckpointRequest();
        request.setPackageId(1L);
        request.setLocationId("loc-1");
        request.setCheckpointTime(now);
        request.setResult(CheckpointResult.OK);
        request.setComment("Passage OK");
        request.setCreatedBy("agent-1");

        // packageId, checkpointAvailable
        availableLocation = new LocationAvailabilityPort.LocationAvailability(1L, true);
        unavailableLocation = new LocationAvailabilityPort.LocationAvailability(1L, false);
    }

    @Test
    void create_shouldSaveCheckpoint_whenLocationAvailable() {
        when(locationAvailabilityPort.getLocation("loc-1")).thenReturn(availableLocation);
        when(mapper.toEntity(request)).thenReturn(entity);
        when(repository.save(entity)).thenReturn(entity);
        when(mapper.toDto(entity)).thenReturn(dto);

        CheckpointLogDto result = service.create(request);

        assertThat(result).isEqualTo(dto);
        verify(locationAvailabilityPort).getLocation("loc-1");
        verify(repository).save(entity);
    }

    @Test
    void create_shouldThrow_whenCheckpointNotAvailable() {
        when(locationAvailabilityPort.getLocation("loc-1")).thenReturn(unavailableLocation);

        assertThatThrownBy(() -> service.create(request))
                .isInstanceOf(CheckpointUnavailableException.class)
                .hasMessageContaining("loc-1");

        verify(repository, never()).save(any());
    }

    @Test
    void create_shouldThrow_whenLocationBelongsToAnotherPackage() {
        LocationAvailabilityPort.LocationAvailability otherPackageLocation =
                new LocationAvailabilityPort.LocationAvailability(2L, true);
        when(locationAvailabilityPort.getLocation("loc-1")).thenReturn(otherPackageLocation);

        assertThatThrownBy(() -> service.create(request))
                .isInstanceOf(com.cbs.logistics.security_checkpoint_service.exception.LocationPackageMismatchException.class)
                .hasMessageContaining("colis 2");

        verify(repository, never()).save(any());
    }

    @Test
    void create_shouldPropagate_whenLocationNotFound() {
        when(locationAvailabilityPort.getLocation("loc-1"))
                .thenThrow(new LocationNotFoundException("La localisation demandée n'existe pas"));

        assertThatThrownBy(() -> service.create(request))
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
        when(repository.findAll(pageable)).thenReturn(page);
        when(mapper.toDto(entity)).thenReturn(dto);

        Page<CheckpointLogDto> result = service.getAll(pageable);

        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0)).isEqualTo(dto);
    }

    @Test
    void getByPackageId_shouldReturnPagedCheckpoints() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<CheckpointLog> page = new PageImpl<>(List.of(entity), pageable, 1);
        when(repository.findByPackageIdOrderByCheckpointTimeDesc(1L, pageable)).thenReturn(page);
        when(mapper.toDto(entity)).thenReturn(dto);

        Page<CheckpointLogDto> result = service.getByPackageId(1L, pageable);

        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0)).isEqualTo(dto);
        verify(repository).findByPackageIdOrderByCheckpointTimeDesc(1L, pageable);
    }
}
