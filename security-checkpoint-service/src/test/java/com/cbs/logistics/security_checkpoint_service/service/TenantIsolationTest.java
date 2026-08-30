package com.cbs.logistics.security_checkpoint_service.service;

import com.cbs.logistics.common.security.context.TenantContext;
import com.cbs.logistics.security_checkpoint_service.dto.CheckpointLogDto;
import com.cbs.logistics.security_checkpoint_service.entity.CheckpointLog;
import com.cbs.logistics.security_checkpoint_service.entity.CheckpointResult;
import com.cbs.logistics.security_checkpoint_service.mapper.CheckpointLogMapper;
import com.cbs.logistics.security_checkpoint_service.port.LocationAvailabilityPort;
import com.cbs.logistics.security_checkpoint_service.repository.CheckpointLogRepository;
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

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TenantIsolationTest {

    private static final String TENANT_A = "tenant-alpha";
    private static final String TENANT_B = "tenant-beta";

    @Mock private CheckpointLogRepository repository;
    @Mock private CheckpointLogMapper mapper;
    @Mock private LocationAvailabilityPort locationAvailabilityPort;
    @InjectMocks private CheckpointLogService service;

    private CheckpointLogDto dtoA;

    @BeforeEach
    void setUp() {
        dtoA = new CheckpointLogDto(1L, 1L, "loc-1",
                LocalDateTime.of(2026, 8, 10, 10, 0),
                CheckpointResult.OK, "Passage OK", "agent-1");
    }

    @AfterEach
    void tearDown() { TenantContext.clear(); }

    @Test
    void getAll_shouldOnlyReturnOwnTenantData() {
        TenantContext.setCurrent(TENANT_A);
        var pageable = PageRequest.of(0, 10);
        CheckpointLog entityA = new CheckpointLog();
        entityA.setId(1L);
        entityA.setTenantId(TENANT_A);
        Page<CheckpointLog> page = new PageImpl<>(List.of(entityA), pageable, 1);

        when(repository.findByTenantId(TENANT_A, pageable)).thenReturn(page);
        when(mapper.toDto(entityA)).thenReturn(dtoA);

        var result = service.getAll(pageable);

        assertThat(result.getContent()).hasSize(1);
        verify(repository).findByTenantId(TENANT_A, pageable);
    }

    @Test
    void getAll_shouldReturnEmpty_whenOtherTenantHasData() {
        TenantContext.setCurrent(TENANT_B);
        var pageable = PageRequest.of(0, 10);
        Page<CheckpointLog> emptyPage = new PageImpl<>(List.of(), pageable, 0);

        when(repository.findByTenantId(TENANT_B, pageable)).thenReturn(emptyPage);

        var result = service.getAll(pageable);

        assertThat(result.getContent()).isEmpty();
    }

    @Test
    void getByPackageId_shouldOnlyReturnOwnTenantData() {
        TenantContext.setCurrent(TENANT_A);
        var pageable = PageRequest.of(0, 10);
        CheckpointLog entityA = new CheckpointLog();
        entityA.setId(1L);
        entityA.setPackageId(1L);
        entityA.setTenantId(TENANT_A);
        Page<CheckpointLog> page = new PageImpl<>(List.of(entityA), pageable, 1);

        when(repository.findByPackageIdAndTenantIdOrderByCheckpointTimeDesc(1L, TENANT_A, pageable))
                .thenReturn(page);
        when(mapper.toDto(entityA)).thenReturn(dtoA);

        var result = service.getByPackageId(1L, pageable);

        assertThat(result.getContent()).hasSize(1);
    }

    @Test
    void getByPackageId_shouldReturnEmpty_whenOtherTenantHasData() {
        TenantContext.setCurrent(TENANT_B);
        var pageable = PageRequest.of(0, 10);
        Page<CheckpointLog> emptyPage = new PageImpl<>(List.of(), pageable, 0);

        when(repository.findByPackageIdAndTenantIdOrderByCheckpointTimeDesc(1L, TENANT_B, pageable))
                .thenReturn(emptyPage);

        var result = service.getByPackageId(1L, pageable);

        assertThat(result.getContent()).isEmpty();
    }
}
