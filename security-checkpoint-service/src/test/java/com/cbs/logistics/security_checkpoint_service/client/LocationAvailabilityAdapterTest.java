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
    void getLocation_shouldMapDtoToPortRecord() {
        LocationServiceClient.LocationDto dto =
                new LocationServiceClient.LocationDto("loc-1", 7L, "Paris", "ZONE_A", true);
        when(locationServiceClient.getLocationById("loc-1")).thenReturn(dto);

        LocationAvailabilityPort.LocationAvailability result = adapter.getLocation("loc-1");

        assertThat(result.packageId()).isEqualTo(7L);
        assertThat(result.checkpointAvailable()).isTrue();
        verify(locationServiceClient).getLocationById("loc-1");
    }

    @Test
    void getLocation_shouldMapUnavailableCheckpoint() {
        LocationServiceClient.LocationDto dto =
                new LocationServiceClient.LocationDto("loc-2", 8L, "Lyon", "ZONE_B", false);
        when(locationServiceClient.getLocationById("loc-2")).thenReturn(dto);

        LocationAvailabilityPort.LocationAvailability result = adapter.getLocation("loc-2");

        assertThat(result.packageId()).isEqualTo(8L);
        assertThat(result.checkpointAvailable()).isFalse();
    }
}
