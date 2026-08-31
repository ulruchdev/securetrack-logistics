package com.cbs.logistics.location_service.service;
import com.cbs.logistics.common.security.context.TenantContext;

import com.cbs.logistics.location_service.client.PackageServiceClient;
import com.cbs.logistics.location_service.dto.CreateLocationRequest;
import com.cbs.logistics.location_service.dto.EnrichedLocationDto;
import com.cbs.logistics.location_service.dto.LocationDto;
import com.cbs.logistics.common.dto.PackageDto;
import com.cbs.logistics.location_service.entity.Location;
import com.cbs.logistics.location_service.exception.LocationNotFoundException;
import com.cbs.logistics.location_service.exception.PackageNotFoundException;
import com.cbs.logistics.location_service.exception.PackageServiceUnavailableException;
import com.cbs.logistics.location_service.locationMapper.LocationMapper;
import com.cbs.logistics.location_service.repository.LocationRepository;
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
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LocationServiceTest {

    @Mock
    private LocationRepository locationRepository;

    @Mock
    private LocationMapper locationMapper;

    @Mock
    private PackageServiceClient packageServiceClient;

    @InjectMocks
    private LocationService locationService;

    private Location location;
    private LocationDto locationDto;
    private CreateLocationRequest createRequest;
    private PackageDto packageDto;

    @BeforeEach
    void setUp() {
        TenantContext.setCurrent("test-tenant");
        location = Location.builder()
                .locationId("loc-1")
                .packageId(1L)
                .city("Paris")
                .zone("ZONE_A")
                .checkpointAvailable(true)
                .build();

        locationDto = new LocationDto("loc-1", 1L, "Paris", "ZONE_A", true);

        createRequest = new CreateLocationRequest();
        createRequest.setPackageId(1L);
        createRequest.setCity("Paris");
        createRequest.setZone("ZONE_A");
        createRequest.setCheckpointAvailable(true);

        packageDto = new PackageDto(1L, "ST-ABCDEF12", "Colis test", "Colis", "STANDARD", 2.5, false, "NEW");
    }

    @Test
    void create_shouldValidatePackageThenSave() {
        when(packageServiceClient.getPackageById(1L)).thenReturn(packageDto);
        when(locationMapper.toEntity(createRequest)).thenReturn(location);
        when(locationRepository.save(location)).thenReturn(location);
        when(locationMapper.toDto(location)).thenReturn(locationDto);

        LocationDto result = locationService.create(createRequest);

        assertThat(result).isEqualTo(locationDto);
        verify(packageServiceClient).getPackageById(1L);
        verify(locationRepository).save(location);
    }

    @Test
    void create_shouldThrowPackageNotFound_whenPackageDoesNotExist() {
        when(packageServiceClient.getPackageById(1L))
                .thenThrow(new PackageNotFoundException("Le colis demandé n'existe pas"));

        assertThatThrownBy(() -> locationService.create(createRequest))
                .isInstanceOf(PackageNotFoundException.class)
                .hasMessage("Le colis demandé n'existe pas");

        verify(locationRepository, never()).save(any());
    }

    @Test
    void create_shouldThrowPackageServiceUnavailable_whenClientFails() {
        when(packageServiceClient.getPackageById(1L))
                .thenThrow(new PackageServiceUnavailableException("Le service de colis est indisponible"));

        assertThatThrownBy(() -> locationService.create(createRequest))
                .isInstanceOf(PackageServiceUnavailableException.class);

        verify(locationRepository, never()).save(any());
    }

    @Test
    void getById_shouldReturnLocation() {
        when(locationRepository.findByLocationIdAndTenantId("loc-1", "test-tenant")).thenReturn(Optional.of(location));
        when(locationMapper.toDto(location)).thenReturn(locationDto);

        LocationDto result = locationService.getById("loc-1");

        assertThat(result).isEqualTo(locationDto);
    }

    @Test
    void getById_shouldThrow_whenLocationNotFound() {
        when(locationRepository.findByLocationIdAndTenantId("loc-1", "test-tenant")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> locationService.getById("loc-1"))
                .isInstanceOf(LocationNotFoundException.class);
    }

    @Test
    void getAll_shouldReturnPagedLocations() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<Location> page = new PageImpl<>(List.of(location), pageable, 1);
        when(locationRepository.findByTenantId("test-tenant", pageable)).thenReturn(page);
        when(locationMapper.toDto(location)).thenReturn(locationDto);

        Page<LocationDto> result = locationService.getAll(pageable);

        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0)).isEqualTo(locationDto);
    }

    @Test
    void getByPackageId_shouldReturnEnrichedLocation() {
        when(locationRepository.findByPackageIdAndTenantId(1L, "test-tenant")).thenReturn(Optional.of(location));
        when(locationMapper.toDto(location)).thenReturn(locationDto);
        when(packageServiceClient.getPackageById(1L)).thenReturn(packageDto);

        EnrichedLocationDto result = locationService.getByPackageId(1L);

        assertThat(result.location()).isEqualTo(locationDto);
        assertThat(result.packageInfo()).isEqualTo(packageDto);
    }

    @Test
    void getByPackageId_shouldThrow_whenLocationNotFound() {
        when(locationRepository.findByPackageIdAndTenantId(1L, "test-tenant")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> locationService.getByPackageId(1L))
                .isInstanceOf(LocationNotFoundException.class);
    }
}
