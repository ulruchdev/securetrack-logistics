package com.cbs.logistics.package_service.controller;

import com.cbs.logistics.package_service.dto.CreatePackageRequest;
import com.cbs.logistics.common.dto.PackageDto;
import com.cbs.logistics.package_service.dto.UpdatePackageRequest;
import com.cbs.logistics.package_service.entity.PackageStatus;
import com.cbs.logistics.package_service.exception.PackageNotFoundException;
import com.cbs.logistics.package_service.service.PackageService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(PackageController.class)
@AutoConfigureMockMvc(addFilters = false)
class PackageControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private PackageService packageService;

    private static final String VALID_CREATE_BODY = """
            {
              "description": "Colis fragile",
              "packageName": "Colis test",
              "packageType": "STANDARD",
              "weight": 2.5,
              "fragile": true
            }
            """;

    private PackageDto dto() {
        return new PackageDto(1L, "ST-ABCDEF12", "Colis fragile", "Colis test", "STANDARD", 2.5, true, "NEW");
    }

    @Test
    void createPackage_shouldReturn201() throws Exception {
        when(packageService.create(any(CreatePackageRequest.class))).thenReturn(dto());

        mockMvc.perform(post("/api/packages")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_CREATE_BODY))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "/api/packages/1"))
                .andExpect(jsonPath("$.packageId").value(1))
                .andExpect(jsonPath("$.packageStatus").value("NEW"));
    }

    @Test
    void createPackage_shouldReturn400_whenValidationFails() throws Exception {
        String invalidBody = """
                {
                  "description": "",
                  "packageName": "Colis test",
                  "packageType": "STANDARD",
                  "weight": -5.0,
                  "fragile": true
                }
                """;

        mockMvc.perform(post("/api/packages")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidBody))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Erreur de validation"))
                .andExpect(jsonPath("$.fieldErrors[?(@.field == 'description')].message").value("La description est obligatoire"))
                .andExpect(jsonPath("$.fieldErrors[?(@.field == 'weight')].message").value("Le poids doit être positif ou nul"));
    }

    @Test
    void getPackageById_shouldReturn200() throws Exception {
        when(packageService.getById(1L)).thenReturn(dto());

        mockMvc.perform(get("/api/packages/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.packageName").value("Colis test"));
    }

    @Test
    void getPackageById_shouldReturn404_whenNotFound() throws Exception {
        when(packageService.getById(99L)).thenThrow(new PackageNotFoundException("Package not found with id: 99"));

        mockMvc.perform(get("/api/packages/99"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.title").value("Colis non trouvé"))
                .andExpect(jsonPath("$.detail").value("Package not found with id: 99"));
    }

    @Test
    void getPackageById_shouldReturn400_whenIdNotNumeric() throws Exception {
        mockMvc.perform(get("/api/packages/abc"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Paramètre invalide"))
                .andExpect(jsonPath("$.status").value(400));
    }

    @Test
    void createPackage_shouldReturn400_whenBodyMalformed() throws Exception {
        mockMvc.perform(post("/api/packages")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"description\": \"incomplet"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Requête invalide"))
                .andExpect(jsonPath("$.status").value(400));
    }

    @Test
    void getAllPackages_shouldReturnPaged200() throws Exception {
        PageImpl<PackageDto> page = new PageImpl<>(List.of(dto()), PageRequest.of(0, 10), 1);
        when(packageService.getAll(any())).thenReturn(page);

        mockMvc.perform(get("/api/packages")
                        .param("page", "0")
                        .param("size", "10")
                        .param("sortBy", "packageId")
                        .param("sortDir", "asc"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].packageId").value(1))
                .andExpect(jsonPath("$.totalElements").value(1));
    }

    @Test
    void getAllPackages_shouldReturn400_whenSizeTooLarge() throws Exception {
        mockMvc.perform(get("/api/packages")
                        .param("page", "0")
                        .param("size", "1000"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Requête invalide"))
                .andExpect(jsonPath("$.detail").value("Le paramètre 'size' doit être compris entre 1 et 100"));
    }

    @Test
    void getAllPackages_shouldReturn400_whenPageNegative() throws Exception {
        mockMvc.perform(get("/api/packages")
                        .param("page", "-1")
                        .param("size", "10"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void getAllPackages_shouldReturn400_whenSortByInvalid() throws Exception {
        mockMvc.perform(get("/api/packages")
                        .param("sortBy", "hack' OR 1=1"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Requête invalide"))
                .andExpect(jsonPath("$.detail").value("Le paramètre 'sortBy' est invalide : hack' OR 1=1"));
    }

    @Test
    void getAllPackages_shouldReturn400_whenSortDirInvalid() throws Exception {
        mockMvc.perform(get("/api/packages")
                        .param("sortDir", "sideways"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Requête invalide"))
                .andExpect(jsonPath("$.detail").value("Le paramètre 'sortDir' doit être 'asc' ou 'desc'"));
    }

    @Test
    void getAllPackages_shouldReturn200_whenSortDirDesc() throws Exception {
        PageImpl<PackageDto> page = new PageImpl<>(List.of(dto()), PageRequest.of(0, 10), 1);
        when(packageService.getAll(any())).thenReturn(page);

        mockMvc.perform(get("/api/packages")
                        .param("sortBy", "packageName")
                        .param("sortDir", "DESC"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].packageId").value(1));
    }

    @Test
    void updatePackage_shouldReturn200() throws Exception {
        // Le service retourne le DTO après application de l'update (statut IN_TRANSIT)
        PackageDto updatedDto = new PackageDto(1L, "ST-ABCDEF12", "Colis fragile", "Colis test", "STANDARD", 2.5, true, "IN_TRANSIT");
        when(packageService.update(eq(1L), any(UpdatePackageRequest.class))).thenReturn(updatedDto);

        mockMvc.perform(patch("/api/packages/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "packageStatus": "IN_TRANSIT"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.packageStatus").value("IN_TRANSIT"));
    }

    @Test
    void updatePackage_shouldReturn400_whenWeightNegative() throws Exception {
        mockMvc.perform(patch("/api/packages/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "weight": -1.0
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors[?(@.field == 'weight')].message").value("Le poids doit être positif ou nul"));
    }

    @Test
    void deletePackage_shouldReturn204() throws Exception {
        doNothing().when(packageService).delete(1L);

        mockMvc.perform(delete("/api/packages/1"))
                .andExpect(status().isNoContent());
    }
}
