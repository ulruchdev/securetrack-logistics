package com.cbs.logistics.location_service.controller;

import com.cbs.logistics.location_service.dto.CheckpointDto;
import com.cbs.logistics.location_service.service.CheckpointService;
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

@WebMvcTest(CheckpointController.class)
@AutoConfigureMockMvc(addFilters = false)
class CheckpointControllerTest {

    @Autowired private MockMvc mockMvc;
    @MockitoBean private CheckpointService checkpointService;

    @Test
    void create_shouldReturn201() throws Exception {
        CheckpointDto dto = new CheckpointDto(1L, 1L, "Gate A", true);
        when(checkpointService.create(any())).thenReturn(dto);

        mockMvc.perform(post("/api/checkpoints")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"siteId\":1,\"name\":\"Gate A\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("Gate A"));
    }

    @Test
    void getById_shouldReturn200() throws Exception {
        CheckpointDto dto = new CheckpointDto(1L, 1L, "Gate A", true);
        when(checkpointService.getById(1L)).thenReturn(dto);

        mockMvc.perform(get("/api/checkpoints/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Gate A"));
    }

    @Test
    void getAll_shouldReturn200() throws Exception {
        CheckpointDto dto = new CheckpointDto(1L, 1L, "Gate A", true);
        when(checkpointService.getAll(any())).thenReturn(new PageImpl<>(List.of(dto), PageRequest.of(0, 10), 1));

        mockMvc.perform(get("/api/checkpoints").param("page", "0").param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].name").value("Gate A"));
    }

    @Test
    void getBySiteId_shouldReturn200() throws Exception {
        CheckpointDto dto = new CheckpointDto(1L, 1L, "Gate A", true);
        when(checkpointService.getBySiteId(1L)).thenReturn(List.of(dto));

        mockMvc.perform(get("/api/checkpoints/by-site/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("Gate A"));
    }

    @Test
    void update_shouldReturn200() throws Exception {
        CheckpointDto dto = new CheckpointDto(1L, 1L, "Gate B", true);
        when(checkpointService.update(any(), any())).thenReturn(dto);

        mockMvc.perform(put("/api/checkpoints/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"siteId\":1,\"name\":\"Gate B\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Gate B"));
    }

    @Test
    void delete_shouldReturn204() throws Exception {
        mockMvc.perform(delete("/api/checkpoints/1"))
                .andExpect(status().isNoContent());
    }
}
