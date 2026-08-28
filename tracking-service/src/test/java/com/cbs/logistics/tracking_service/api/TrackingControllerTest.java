package com.cbs.logistics.tracking_service.api;

import com.cbs.logistics.tracking_service.command.RegisterTransitionCommand;
import com.cbs.logistics.tracking_service.exception.InvalidTransitionException;
import com.cbs.logistics.tracking_service.exception.GlobalExceptionHandler;
import org.axonframework.commandhandling.gateway.CommandGateway;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Tests HTTP du flux d'ÉCRITURE : 201 nominal, 400 validation, 409 métier.
 * Le contrat d'erreur vérifié est ProblemDetail RFC 7807 (application/problem+json).
 * La sécurité est désactivée dans les tests unitaires (addFilters = false).
 */
@WebMvcTest(TrackingController.class)
@AutoConfigureMockMvc(addFilters = false)
class TrackingControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private CommandGateway commandGateway;

    @Test
    @DisplayName("POST valide : 201 et la commande contient les champs de la requête")
    void post_valid_shouldReturn201AndSendCommand() throws Exception {
        mockMvc.perform(post("/api/tracking")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"packageId":"PKG-123","locationId":"LOC-Lyon","newStatus":"IN_TRANSIT"}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.packageId").value("PKG-123"))
                .andExpect(jsonPath("$.status").value("IN_TRANSIT"));

        verify(commandGateway).sendAndWait(any(RegisterTransitionCommand.class));
    }

    @Test
    @DisplayName("POST sans packageId : 400 ProblemDetail avec fieldErrors")
    void post_missingPackageId_shouldReturn400() throws Exception {
        mockMvc.perform(post("/api/tracking")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"newStatus":"NEW"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Erreur de validation"))
                .andExpect(jsonPath("$.fieldErrors[0].field").value("packageId"));
    }

    @Test
    @DisplayName("Invariant métier violé : l'exception de l'aggregate devient un 409")
    void post_deliveredInvariant_shouldReturn409ProblemDetail() throws Exception {
        doThrow(new InvalidTransitionException("Le colis PKG-1 est déjà DELIVERED"))
                .when(commandGateway).sendAndWait(any(RegisterTransitionCommand.class));

        mockMvc.perform(post("/api/tracking")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"packageId":"PKG-1","newStatus":"NEW"}
                                """))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.title").value("Transition invalide"))
                .andExpect(jsonPath("$.detail").value("Le colis PKG-1 est déjà DELIVERED"));
    }
}
