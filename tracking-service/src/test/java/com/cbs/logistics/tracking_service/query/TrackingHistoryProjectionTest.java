package com.cbs.logistics.tracking_service.query;

import com.cbs.logistics.tracking_service.event.TrackingTransitionedEvent;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;

/**
 * Test de la PROJECTION : un événement reçu doit produire exactement une
 * ligne dans le read-model, avec les champs de l'événement.
 */
@ExtendWith(MockitoExtension.class)
class TrackingHistoryProjectionTest {

    @Mock
    private TrackingHistoryRepository repository;

    @InjectMocks
    private TrackingHistoryProjection projection;

    @Test
    @DisplayName("@EventHandler : l'événement est projeté en ligne du read-model")
    void onEvent_shouldSaveEntryWithEventFields() {
        Instant when = Instant.parse("2026-08-26T12:00:00Z");
        TrackingTransitionedEvent event =
                new TrackingTransitionedEvent("PKG-123", "LOC-Lyon", "IN_TRANSIT", when);

        projection.on(event);

        ArgumentCaptor<TrackingHistoryEntry> captor = ArgumentCaptor.forClass(TrackingHistoryEntry.class);
        verify(repository).save(captor.capture());
        TrackingHistoryEntry saved = captor.getValue();
        assertThat(saved.getPackageId()).isEqualTo("PKG-123");
        assertThat(saved.getLocationId()).isEqualTo("LOC-Lyon");
        assertThat(saved.getStatus()).isEqualTo("IN_TRANSIT");
        assertThat(saved.getOccurredAt()).isEqualTo(when);
    }
}
