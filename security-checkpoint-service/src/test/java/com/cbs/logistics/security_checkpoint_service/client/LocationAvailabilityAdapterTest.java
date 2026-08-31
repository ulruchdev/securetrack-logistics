package com.cbs.logistics.security_checkpoint_service.client;

import com.cbs.logistics.security_checkpoint_service.port.LocationAvailabilityPort;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LocationAvailabilityAdapterTest {

    @Mock
    private LocationServiceClient locationServiceClient;

    @InjectMocks
    private LocationAvailabilityAdapter adapter;

    @Test
    void getCheckpointAvailability_shouldMapDtoToPortRecord() {
        LocationServiceClient.CheckpointDto dto =
                new LocationServiceClient.CheckpointDto(10L, 1L, "Checkpoint A", true);
        when(locationServiceClient.getCheckpointById(10L)).thenReturn(dto);

        LocationAvailabilityPort.CheckpointAvailability result = adapter.getCheckpointAvailability(10L);

        assertThat(result.active()).isTrue();
        assertThat(result.siteId()).isEqualTo(1L);
        verify(locationServiceClient).getCheckpointById(10L);
    }

    @Test
    void getCheckpointAvailability_shouldMapUnavailableCheckpoint() {
        LocationServiceClient.CheckpointDto dto =
                new LocationServiceClient.CheckpointDto(20L, 2L, "Checkpoint B", false);
        when(locationServiceClient.getCheckpointById(20L)).thenReturn(dto);

        LocationAvailabilityPort.CheckpointAvailability result = adapter.getCheckpointAvailability(20L);

        assertThat(result.active()).isFalse();
        assertThat(result.siteId()).isEqualTo(2L);
    }
}
