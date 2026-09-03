package com.cbs.logistics.security_checkpoint_service.config;

import com.cbs.logistics.security_checkpoint_service.controller.CheckpointLogController;
import com.cbs.logistics.security_checkpoint_service.dto.CheckpointLogDto;
import com.cbs.logistics.security_checkpoint_service.entity.CheckpointResult;
import com.cbs.logistics.security_checkpoint_service.service.CheckpointLogService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(CheckpointLogController.class)
@AutoConfigureMockMvc(addFilters = false)
class SecurityConfigTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private CheckpointLogService service;

    @Test
    void endpointAccessible_whenFiltersDisabled() throws Exception {
        CheckpointLogDto dto = new CheckpointLogDto(
                1L, "ST-ABCDEF12", 10L,
                LocalDateTime.of(2026, 8, 10, 10, 0),
                CheckpointResult.OK, "Passage OK", "agent-1");
        PageImpl<CheckpointLogDto> page = new PageImpl<>(List.of(dto), PageRequest.of(0, 10), 1);
        when(service.getAll(any())).thenReturn(page);

        mockMvc.perform(get("/api/checkpoints"))
                .andExpect(status().isOk());
    }
}
