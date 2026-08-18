package com.cbs.logistics.location_service.controller;

import com.cbs.logistics.location_service.dto.CreateLocationRequest;
import com.cbs.logistics.location_service.dto.EnrichedLocationDto;
import com.cbs.logistics.location_service.dto.LocationDto;
import com.cbs.logistics.common.dto.PackageDto;
import com.cbs.logistics.location_service.exception.LocationNotFoundException;
import com.cbs.logistics.location_service.exception.PackageNotFoundException;
import com.cbs.logistics.location_service.exception.PackageServiceUnavailableException;
import feign.FeignException;
import com.cbs.logistics.location_service.service.LocationService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(LocationController.class)
class LocationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private LocationService locationService;

    private static FeignException feignException(int status) {
        return FeignException.errorStatus(
                "POST",
                feign.Response.builder()
                        .status(status)
                        .reason("error")
                        .request(feign.Request.create(
                                feign.Request.HttpMethod.POST,
                                "/api/locations",
                                java.util.Collections.emptyMap(),
                                null,
                                feign.Util.UTF_8,
                                null))
                        .build());
    }

    private static final String VALID_BODY = """
            {
              "packageId": 1,
              "city": "Paris",
              "zone": "ZONE_A",
              "checkpointAvailable": true
            }
            """;

    @Test
    void createLocation_shouldReturn201() throws Exception {
        LocationDto dto = new LocationDto("loc-1", 1L, "Paris", "ZONE_A", true);
        when(locationService.create(any(CreateLocationRequest.class))).thenReturn(dto);

        mockMvc.perform(post("/api/locations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_BODY))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.locationId").value("loc-1"))
                .andExpect(jsonPath("$.packageId").value(1))
                .andExpect(jsonPath("$.city").value("Paris"));
    }

    @Test
    void createLocation_shouldReturn400_whenValidationFails() throws Exception {
        String invalidBody = """
                {
                  "packageId": null,
                  "city": "",
                  "zone": "ZONE_A",
                  "checkpointAvailable": true
                }
                """;

        mockMvc.perform(post("/api/locations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidBody))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Erreur de validation"))
                .andExpect(jsonPath("$.fieldErrors.packageId").value("L'ID du package est obligatoire"))
                .andExpect(jsonPath("$.fieldErrors.city").value("La ville est obligatoire"));
    }

    @Test
    void createLocation_shouldReturn409_whenDuplicatePackage() throws Exception {
        when(locationService.create(any(CreateLocationRequest.class)))
                .thenThrow(new DuplicateKeyException("duplicate"));

        mockMvc.perform(post("/api/locations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_BODY))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.title").value("Conflit de données"));
    }

    @Test
    void createLocation_shouldReturn404_whenPackageNotFound() throws Exception {
        when(locationService.create(any(CreateLocationRequest.class)))
                .thenThrow(new PackageNotFoundException("Le colis demandé n'existe pas"));

        mockMvc.perform(post("/api/locations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_BODY))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.title").value("Colis non trouvé"));
    }

    @Test
    void createLocation_shouldReturn503_whenFeignException() throws Exception {
        when(locationService.create(any(CreateLocationRequest.class)))
                .thenThrow(feignException(503));

        mockMvc.perform(post("/api/locations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_BODY))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.title").value("Service externe indisponible"));
    }

    @Test
    void createLocation_shouldReturn500_whenUnexpectedError() throws Exception {
        when(locationService.create(any(CreateLocationRequest.class)))
                .thenThrow(new IllegalStateException("boom"));

        mockMvc.perform(post("/api/locations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_BODY))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.title").value("Erreur interne du serveur"));
    }

    @Test
    void createLocation_shouldReturn503_whenPackageServiceUnavailable() throws Exception {
        when(locationService.create(any(CreateLocationRequest.class)))
                .thenThrow(new PackageServiceUnavailableException("Le service de colis est indisponible"));

        mockMvc.perform(post("/api/locations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_BODY))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.title").value("Service de colis indisponible"));
    }

    @Test
    void getLocationById_shouldReturn200() throws Exception {
        LocationDto dto = new LocationDto("loc-1", 1L, "Paris", "ZONE_A", true);
        when(locationService.getById("loc-1")).thenReturn(dto);

        mockMvc.perform(get("/api/locations/loc-1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.zone").value("ZONE_A"))
                .andExpect(jsonPath("$.checkpointAvailable").value(true));
    }

    @Test
    void getLocationById_shouldReturn404_whenNotFound() throws Exception {
        when(locationService.getById("loc-999"))
                .thenThrow(new LocationNotFoundException("Location not found with id: loc-999"));

        mockMvc.perform(get("/api/locations/loc-999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.title").value("Localisation non trouvée"))
                .andExpect(jsonPath("$.detail").value("Location not found with id: loc-999"));
    }

    @Test
    void getAllLocations_shouldReturnPaged200() throws Exception {
        LocationDto dto = new LocationDto("loc-1", 1L, "Paris", "ZONE_A", true);
        PageImpl<LocationDto> page = new PageImpl<>(List.of(dto), PageRequest.of(0, 10), 1);
        when(locationService.getAll(any())).thenReturn(page);

        mockMvc.perform(get("/api/locations")
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].locationId").value("loc-1"))
                .andExpect(jsonPath("$.totalElements").value(1));
    }

    @Test
    void getLocationByPackageId_shouldReturn200() throws Exception {
        LocationDto locationDto = new LocationDto("loc-1", 1L, "Paris", "ZONE_A", true);
        PackageDto packageDto = new PackageDto(1L, "Colis test", "Colis", "STANDARD", 2.5, false, "NEW");
        EnrichedLocationDto enriched = new EnrichedLocationDto(locationDto, packageDto);
        when(locationService.getByPackageId(1L)).thenReturn(enriched);

        mockMvc.perform(get("/api/locations/by-package/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.location.city").value("Paris"))
                .andExpect(jsonPath("$.packageInfo.packageId").value(1))
                .andExpect(jsonPath("$.packageInfo.packageStatus").value("NEW"));
    }

    @Test
    void getLocationByPackageId_shouldReturn404_whenNoLocation() throws Exception {
        when(locationService.getByPackageId(1L))
                .thenThrow(new LocationNotFoundException("Location not found for package id: 1"));

        mockMvc.perform(get("/api/locations/by-package/1"))
                .andExpect(status().isNotFound());
    }

    @Test
    void getLocationByPackageId_shouldReturn400_whenIdNotNumeric() throws Exception {
        mockMvc.perform(get("/api/locations/by-package/abc"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Paramètre invalide"))
                .andExpect(jsonPath("$.status").value(400));
    }

    @Test
    void createLocation_shouldReturn400_whenBodyMalformed() throws Exception {
        mockMvc.perform(post("/api/locations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"packageId\": \"incomplet"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Requête invalide"))
                .andExpect(jsonPath("$.status").value(400));
    }
}
