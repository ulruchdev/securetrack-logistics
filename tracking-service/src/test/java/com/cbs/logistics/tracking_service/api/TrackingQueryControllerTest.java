package com.cbs.logistics.tracking_service.api;

import com.cbs.logistics.tracking_service.exception.NotFoundException;
import com.cbs.logistics.tracking_service.query.FindHistoryQuery;
import com.cbs.logistics.tracking_service.query.FindTransitionByIdQuery;
import com.cbs.logistics.tracking_service.query.TransitionDto;
import org.axonframework.messaging.responsetypes.ResponseType;
import org.axonframework.messaging.responsetypes.ResponseTypes;
import org.axonframework.queryhandling.QueryGateway;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Tests unitaires du flux de LECTURE sans Spring context.
 *
 * <p>Le contrôleur est instancié directement avec un mock pur Mockito.
 * Pas de proxy Spring, pas de piège de default methods Axon.</p>
 */
class TrackingQueryControllerTest {

    private QueryGateway queryGateway;
    private TrackingQueryController controller;

    private final TransitionDto dto =
            new TransitionDto(1L, "PKG-123", "LOC-Lyon", "IN_TRANSIT",
                    Instant.parse("2026-08-26T12:00:00Z"));

    @BeforeEach
    void setUp() {
        queryGateway = mock(QueryGateway.class);
        controller = new TrackingQueryController(queryGateway);
    }

    @SuppressWarnings("unchecked")
    @Test
    @DisplayName("history() retourne la liste des transitions via QueryGateway")
    void history_shouldReturnCompletableFutureWithList() {
        when(queryGateway.query(
                any(FindHistoryQuery.class),
                any(ResponseType.class)))
                .thenReturn(CompletableFuture.completedFuture(List.of(dto)));

        CompletableFuture<List<TransitionDto>> result = controller.history("PKG-123");

        assertNotNull(result);
        assertEquals(1, result.join().size());
        assertEquals("PKG-123", result.join().get(0).packageId());
        verify(queryGateway).query(
                any(FindHistoryQuery.class),
                any(ResponseType.class));
    }

    @SuppressWarnings("unchecked")
    @Test
    @DisplayName("transition() retourne la transition quand elle existe")
    void transition_found_shouldReturnDto() {
        when(queryGateway.query(
                any(FindTransitionByIdQuery.class),
                any(ResponseType.class)))
                .thenReturn(CompletableFuture.completedFuture(dto));

        CompletableFuture<TransitionDto> result = controller.transition(1L);

        assertNotNull(result);
        assertEquals("LOC-Lyon", result.join().locationId());
        verify(queryGateway).query(
                any(FindTransitionByIdQuery.class),
                any(ResponseType.class));
    }

    @SuppressWarnings("unchecked")
    @Test
    @DisplayName("transition() propage laNotFoundException quand introuvable")
    void transition_notFound_shouldPropagateException() {
        when(queryGateway.query(
                any(FindTransitionByIdQuery.class),
                any(ResponseType.class)))
                .thenReturn(CompletableFuture.failedFuture(
                        new NotFoundException("Aucune transition")));

        CompletableFuture<TransitionDto> result = controller.transition(99L);

        assertThrows(java.util.concurrent.CompletionException.class, result::join);
        verify(queryGateway).query(
                any(FindTransitionByIdQuery.class),
                any(ResponseType.class));
    }
}
