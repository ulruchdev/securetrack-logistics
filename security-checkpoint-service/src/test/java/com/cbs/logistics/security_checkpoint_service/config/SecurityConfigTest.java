package com.cbs.logistics.security_checkpoint_service.config;

import com.cbs.logistics.security_checkpoint_service.controller.CheckpointLogController;
import com.cbs.logistics.security_checkpoint_service.dto.CheckpointLogDto;
import com.cbs.logistics.security_checkpoint_service.entity.CheckpointResult;
import com.cbs.logistics.security_checkpoint_service.service.CheckpointLogService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageImpl;
import org.springframework.test.context.TestPropertySource;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Tests de la chaîne de sécurité réelle (SecurityConfig chargée, filtres ACTIFS).
 * Le mot de passe est fourni via @TestPropertySource car il est OBLIGATOIRE
 * (plus aucun défaut en dur — cf. application.yml et SecurityConfig).
 */
@WebMvcTest(CheckpointLogController.class)
@Import(SecurityConfig.class)
@TestPropertySource(properties = {
        "security.checkpoint.password=change-me-please",
        // HTTPS forcé par défaut hors dev : désactivé ici pour les tests MockMvc en HTTP
        "security.checkpoint.require-https=false"
})
class SecurityConfigTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private CheckpointLogService service;

    private static String basicAuth(String username, String password) {
        String token = username + ":" + password;
        return "Basic " + Base64.getEncoder().encodeToString(token.getBytes(StandardCharsets.UTF_8));
    }

    @Test
    void protectedEndpoint_shouldReturn401_withoutCredentials() throws Exception {
        mockMvc.perform(get("/api/checkpoints"))
                .andExpect(status().isUnauthorized())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(header().string("WWW-Authenticate", "Basic realm=\"CBS Logistics\""))
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.title").value("Non autorisé"))
                .andExpect(jsonPath("$.detail").value("Authentification requise : identifiants absents ou invalides"));
    }

    @Test
    void protectedEndpoint_shouldReturn401_withWrongCredentials() throws Exception {
        mockMvc.perform(get("/api/checkpoints")
                        .header("Authorization", basicAuth("admin", "wrong-password")))
                .andExpect(status().isUnauthorized())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(header().string("WWW-Authenticate", "Basic realm=\"CBS Logistics\""))
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.title").value("Non autorisé"));
    }

    @Test
    void protectedEndpoint_shouldReturn200_withValidCredentials() throws Exception {
        CheckpointLogDto dto = new CheckpointLogDto(
                1L, 1L, "loc-1",
                LocalDateTime.of(2026, 8, 10, 10, 0),
                CheckpointResult.OK, "Passage OK", "agent-1");
        PageImpl<CheckpointLogDto> page = new PageImpl<>(List.of(dto), PageRequest.of(0, 10), 1);
        when(service.getAll(any())).thenReturn(page);

        mockMvc.perform(get("/api/checkpoints")
                        .header("Authorization", basicAuth("admin", "change-me-please")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].packageId").value(1));
    }

    @Test
    void swaggerUi_shouldBePermitAll_withoutCredentials() throws Exception {
        // Routes permitAll : la requête atteint le dispatcher sans authentification.
        // (Le statut exact dépend du contexte de test — l'essentiel est l'ABSENCE de 401.)
        mockMvc.perform(get("/swagger-ui/index.html"))
                .andExpect(result ->
                        org.assertj.core.api.Assertions.assertThat(result.getResponse().getStatus())
                                .isNotEqualTo(401));
    }

    @Test
    void apiDocs_shouldBePermitAll_withoutCredentials() throws Exception {
        mockMvc.perform(get("/v3/api-docs").contentType(MediaType.APPLICATION_JSON))
                .andExpect(result ->
                        org.assertj.core.api.Assertions.assertThat(result.getResponse().getStatus())
                                .isNotEqualTo(401));
    }
}
