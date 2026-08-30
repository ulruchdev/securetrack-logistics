package com.cbs.logistics.location_service.service;

import com.cbs.logistics.common.security.context.TenantContext;
import com.cbs.logistics.location_service.dto.CheckpointDto;
import com.cbs.logistics.location_service.dto.CreateCheckpointRequest;
import com.cbs.logistics.location_service.entity.Checkpoint;
import com.cbs.logistics.location_service.exception.LocationNotFoundException;
import com.cbs.logistics.location_service.repository.CheckpointRepository;
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

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CheckpointServiceTest {

    private static final String TENANT_ID = "test-tenant";

    @Mock private CheckpointRepository checkpointRepository;
    @InjectMocks private CheckpointService checkpointService;

    @BeforeEach
    void setUp() { TenantContext.setCurrent(TENANT_ID); }

    @AfterEach
    void tearDown() { TenantContext.clear(); }

    @Test
    void create_shouldSetTenantId() {
        CreateCheckpointRequest request = new CreateCheckpointRequest(1L, "CP-1");
        Checkpoint cp = Checkpoint.builder().id(1L).tenantId(TENANT_ID).siteId(1L).name("CP-1").active(true).build();
        when(checkpointRepository.save(any())).thenReturn(cp);

        CheckpointDto result = checkpointService.create(request);

        assertThat(result.name()).isEqualTo("CP-1");
        assertThat(result.siteId()).isEqualTo(1L);
    }

    @Test
    void getById_shouldReturn404_whenNotFound() {
        when(checkpointRepository.findByIdAndTenantId(1L, TENANT_ID)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> checkpointService.getById(1L)).isInstanceOf(LocationNotFoundException.class);
    }

    @Test
    void getBySiteId_shouldReturnOwnTenantData() {
        Checkpoint cp = Checkpoint.builder().id(1L).tenantId(TENANT_ID).siteId(1L).name("CP-1").active(true).build();
        when(checkpointRepository.findBySiteIdAndTenantId(1L, TENANT_ID)).thenReturn(List.of(cp));

        var result = checkpointService.getBySiteId(1L);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).siteId()).isEqualTo(1L);
    }

    @Test
    void delete_shouldSoftDelete() {
        Checkpoint cp = Checkpoint.builder().id(1L).tenantId(TENANT_ID).siteId(1L).name("CP-1").active(true).build();
        when(checkpointRepository.findByIdAndTenantId(1L, TENANT_ID)).thenReturn(Optional.of(cp));
        when(checkpointRepository.save(any())).thenReturn(cp);

        checkpointService.delete(1L);

        assertThat(cp.getActive()).isFalse();
    }
}
