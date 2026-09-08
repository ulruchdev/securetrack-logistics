package com.cbs.logistics.location_service.controller;

import com.cbs.logistics.location_service.dto.SiteDto;
import com.cbs.logistics.location_service.service.SiteService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(SiteController.class)
@AutoConfigureMockMvc(addFilters = false)
class SiteControllerTest {

    @Autowired private MockMvc mockMvc;
    @MockitoBean private SiteService siteService;

    @Test
    void create_shouldReturn201() throws Exception {
        SiteDto dto = new SiteDto(1L, "Site A", "123 rue", 48.8, 2.3, true);
        when(siteService.create(any())).thenReturn(dto);

        mockMvc.perform(post("/api/sites")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Site A\",\"address\":\"123 rue\",\"latitude\":48.8,\"longitude\":2.3}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("Site A"));
    }

    @Test
    void getById_shouldReturn200() throws Exception {
        SiteDto dto = new SiteDto(1L, "Site A", "123 rue", 48.8, 2.3, true);
        when(siteService.getById(1L)).thenReturn(dto);

        mockMvc.perform(get("/api/sites/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Site A"));
    }

    @Test
    void getAll_shouldReturn200() throws Exception {
        SiteDto dto = new SiteDto(1L, "Site A", "123 rue", 48.8, 2.3, true);
        when(siteService.getAll(any())).thenReturn(new PageImpl<>(List.of(dto), PageRequest.of(0, 10), 1));

        mockMvc.perform(get("/api/sites").param("page", "0").param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].name").value("Site A"));
    }

    @Test
    void update_shouldReturn200() throws Exception {
        SiteDto dto = new SiteDto(1L, "Site B", "456 rue", 49.0, 2.0, true);
        when(siteService.update(any(), any())).thenReturn(dto);

        mockMvc.perform(put("/api/sites/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Site B\",\"address\":\"456 rue\",\"latitude\":49.0,\"longitude\":2.0}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Site B"));
    }

    @Test
    void delete_shouldReturn204() throws Exception {
        mockMvc.perform(delete("/api/sites/1"))
                .andExpect(status().isNoContent());
    }
}
