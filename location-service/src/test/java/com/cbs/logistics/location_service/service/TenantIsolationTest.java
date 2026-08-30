package com.cbs.logistics.location_service.service;

import com.cbs.logistics.common.dto.PackageDto;
import com.cbs.logistics.common.security.context.TenantContext;
import com.cbs.logistics.location_service.client.PackageServiceClient;
import com.cbs.logistics.location_service.dto.CreateLocationRequest;
import com.cbs.logistics.location_service.dto.LocationDto;
import com.cbs.logistics.location_service.entity.Location;
import com.cbs.logistics.location_service.exception.LocationNotFoundException;
import com.cbs.logistics.location_service.locationMapper.LocationMapper;
import com.cbs.logistics.location_service.repository.LocationRepository;
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
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TenantIsolationTest {

    private static final String TENANT_A = "tenant-alpha";
    private static final String TENANT_B = "tenant-beta";

    @Mock
    private LocationRepository locationRepository;

    @Mock
    private LocationMapper locationMapper;

    @Mock
    private PackageServiceClient packageServiceClient;

    @InjectMocks
    private LocationService locationService;

    private LocationDto dtoA;

    @BeforeEach
    void setUp() {
        dtoA = new LocationDto("loc-1", 1L, "Paris", "Zone A", true);
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }

    @Test
    void getById_shouldReturn404_whenAccessingOtherTenantData() {
        TenantContext.setCurrent(TENANT_B);
        when(locationRepository.findByLocationIdAndTenantId("loc-1", TENANT_B))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> locationService.getById("loc-1"))
                .isInstanceOf(LocationNotFoundException.class);
    }

    @Test
    void getById_shouldReturnLocation_whenAccessingOwnTenantData() {
        Location location = new Location();
        location.setLocationId("loc-1");
        location.setTenantId(TENANT_A);

        TenantContext.setCurrent(TENANT_A);
        when(locationRepository.findByLocationIdAndTenantId("loc-1", TENANT_A))
                .thenReturn(Optional.of(location));
        when(locationMapper.toDto(location)).thenReturn(dtoA);

        LocationDto result = locationService.getById("loc-1");

        assertThat(result.locationId()).isEqualTo("loc-1");
    }

    @Test
    void getByPackageId_shouldReturn404_whenAccessingOtherTenantData() {
        TenantContext.setCurrent(TENANT_B);
        when(locationRepository.findByPackageIdAndTenantId(1L, TENANT_B))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> locationService.getByPackageId(1L))
                .isInstanceOf(LocationNotFoundException.class);
    }

    @Test
    void getAll_shouldOnlyReturnOwnTenantData() {
        Location locationA = new Location();
        locationA.setLocationId("loc-1");
        locationA.setTenantId(TENANT_A);

        TenantContext.setCurrent(TENANT_A);
        var pageable = PageRequest.of(0, 10);
        Page<Location> page = new PageImpl<>(List.of(locationA), pageable, 1);

        when(locationRepository.findByTenantId(TENANT_A, pageable)).thenReturn(page);
        when(locationMapper.toDto(locationA)).thenReturn(dtoA);

        var result = locationService.getAll(pageable);

        assertThat(result.getContent()).hasSize(1);
        verify(locationRepository).findByTenantId(TENANT_A, pageable);
    }

    @Test
    void getAll_shouldReturnEmpty_whenOtherTenantHasData() {
        TenantContext.setCurrent(TENANT_B);
        var pageable = PageRequest.of(0, 10);
        Page<Location> emptyPage = new PageImpl<>(List.of(), pageable, 0);

        when(locationRepository.findByTenantId(TENANT_B, pageable)).thenReturn(emptyPage);

        var result = locationService.getAll(pageable);

        assertThat(result.getContent()).isEmpty();
    }

    @Test
    void create_shouldSetTenantId_fromJwt() {
        Location location = new Location();
        location.setLocationId("loc-2");
        location.setTenantId(TENANT_A);

        TenantContext.setCurrent(TENANT_A);
        CreateLocationRequest request = new CreateLocationRequest();
        request.setPackageId(2L);
        request.setCity("Lyon");

        when(packageServiceClient.getPackageById(anyLong())).thenReturn(new PackageDto(2L, "pkg", null, null, 1.0, false, "NEW"));
        when(locationMapper.toEntity(request)).thenReturn(location);
        when(locationRepository.save(any())).thenReturn(location);
        when(locationMapper.toDto(any())).thenReturn(dtoA);

        locationService.create(request);

        assertThat(location.getTenantId()).isEqualTo(TENANT_A);
    }
}
