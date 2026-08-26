package com.cbs.logistics.security_checkpoint_service.controller;

import com.cbs.logistics.security_checkpoint_service.dto.CheckpointLogDto;
import com.cbs.logistics.security_checkpoint_service.dto.CreateCheckpointRequest;
import com.cbs.logistics.security_checkpoint_service.entity.CheckpointResult;
import com.cbs.logistics.security_checkpoint_service.exception.CheckpointLogNotFoundException;
import com.cbs.logistics.security_checkpoint_service.exception.CheckpointUnavailableException;
import com.cbs.logistics.security_checkpoint_service.exception.LocationNotFoundException;
import com.cbs.logistics.security_checkpoint_service.exception.LocationServiceUnavailableException;
import feign.FeignException;
import com.cbs.logistics.security_checkpoint_service.service.CheckpointLogService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(CheckpointLogController.class)
@AutoConfigureMockMvc(addFilters = false) // Spring Security testé séparément ; on désactive les filtres ici
class CheckpointLogControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private CheckpointLogService service;

    private static final String VALID_BODY = """
            {
              "packageId": 1,
              "locationId": "loc-1",
              "result": "OK",
              "comment": "Passage OK",
              "createdBy": "agent-1"
            }
            """;

    private CheckpointLogDto dto() {
        return new CheckpointLogDto(
                1L, 1L, "loc-1",
                LocalDateTime.of(2026, 8, 10, 10, 0),
                CheckpointResult.OK, "Passage OK", "agent-1");
    }

    @Test
    void createCheckpointLog_shouldReturn201() throws Exception {
        when(service.create(any(CreateCheckpointRequest.class))).thenReturn(dto());

        mockMvc.perform(post("/api/checkpoints")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_BODY))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "/api/checkpoints/1"))
                .andExpect(jsonPath("$.packageId").value(1))
                .andExpect(jsonPath("$.result").value("OK"));
    }

    @Test
    void createCheckpointLog_shouldReturn400_whenValidationFails() throws Exception {
        String invalidBody = """
                {
                  "packageId": null,
                  "locationId": "",
                  "result": null,
                  "createdBy": ""
                }
                """;

        mockMvc.perform(post("/api/checkpoints")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidBody))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Erreur de validation"))
                .andExpect(jsonPath("$.fieldErrors[?(@.field == 'packageId')].message").value("L'ID du colis est obligatoire"));
    }

    @Test
    void createCheckpointLog_shouldReturn404_whenLocationNotFound() throws Exception {
        when(service.create(any(CreateCheckpointRequest.class)))
                .thenThrow(new LocationNotFoundException("La localisation demandée n'existe pas"));

        mockMvc.perform(post("/api/checkpoints")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_BODY))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.title").value("Localisation non trouvée"));
    }

    @Test
    void createCheckpointLog_shouldReturn503_whenLocationServiceUnavailable() throws Exception {
        when(service.create(any(CreateCheckpointRequest.class)))
                .thenThrow(new LocationServiceUnavailableException("Le service de localisation est indisponible"));

        mockMvc.perform(post("/api/checkpoints")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_BODY))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.title").value("Service de localisation indisponible"));
    }

    @Test
    void createCheckpointLog_shouldReturn503_whenFeignException() throws Exception {
        when(service.create(any(CreateCheckpointRequest.class)))
                .thenThrow(feignException(503));

        mockMvc.perform(post("/api/checkpoints")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_BODY))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.title").value("Service externe indisponible"));
    }

    @Test
    void createCheckpointLog_shouldReturn500_whenUnexpectedError() throws Exception {
        when(service.create(any(CreateCheckpointRequest.class)))
                .thenThrow(new IllegalStateException("boom"));

        mockMvc.perform(post("/api/checkpoints")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_BODY))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.title").value("Erreur interne du serveur"));
    }

    @Test
    void createCheckpointLog_shouldReturn422_whenCheckpointUnavailable() throws Exception {
        when(service.create(any(CreateCheckpointRequest.class)))
                .thenThrow(new CheckpointUnavailableException("Checkpoint not available for location: loc-1"));

        mockMvc.perform(post("/api/checkpoints")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_BODY))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.title").value("Checkpoint non disponible"));
    }

    private static FeignException feignException(int status) {
        return FeignException.errorStatus(
                "POST",
                feign.Response.builder()
                        .status(status)
                        .reason("error")
                        .request(feign.Request.create(
                                feign.Request.HttpMethod.POST,
                                "/api/checkpoints",
                                java.util.Collections.emptyMap(),
                                null,
                                feign.Util.UTF_8,
                                null))
                        .build());
    }

    @Test
    void getCheckpointLogById_shouldReturn200() throws Exception {
        when(service.getById(1L)).thenReturn(dto());

        mockMvc.perform(get("/api/checkpoints/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.locationId").value("loc-1"));
    }

    @Test
    void getCheckpointLogById_shouldReturn404_whenNotFound() throws Exception {
        when(service.getById(99L)).thenThrow(new CheckpointLogNotFoundException(99L));

        mockMvc.perform(get("/api/checkpoints/99"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.title").value("Log de checkpoint non trouvé"))
                .andExpect(jsonPath("$.detail").value("Checkpoint log not found with id: 99"));
    }

    @Test
    void getAllCheckpointLogs_shouldReturnPaged200() throws Exception {
        PageImpl<CheckpointLogDto> page = new PageImpl<>(List.of(dto()), PageRequest.of(0, 10), 1);
        when(service.getAll(any())).thenReturn(page);

        mockMvc.perform(get("/api/checkpoints")
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].packageId").value(1))
                .andExpect(jsonPath("$.totalElements").value(1));
    }

    @Test
    void getCheckpointLogsByPackageId_shouldReturnPaged200() throws Exception {
        PageImpl<CheckpointLogDto> page = new PageImpl<>(List.of(dto()), PageRequest.of(0, 10), 1);
        when(service.getByPackageId(eq(1L), any())).thenReturn(page);

        mockMvc.perform(get("/api/checkpoints/by-package/1")
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].result").value("OK"));
    }
}
