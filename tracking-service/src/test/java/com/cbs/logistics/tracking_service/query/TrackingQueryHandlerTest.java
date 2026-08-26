package com.cbs.logistics.tracking_service.query;

import com.cbs.logistics.tracking_service.exception.NotFoundException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

/** Tests des @QueryHandler (côté lecture pur, sans Axon runtime). */
@ExtendWith(MockitoExtension.class)
class TrackingQueryHandlerTest {

    @Mock
    private TrackingHistoryRepository repository;

    @InjectMocks
    private TrackingQueryHandler handler;

    private final TrackingHistoryEntry entry = new TrackingHistoryEntry(
            "PKG-1", "LOC-A", "NEW", Instant.parse("2026-08-26T10:00:00Z"));

    @Test
    @DisplayName("FindHistoryQuery : retourne l'historique mappé en DTO")
    void history_shouldReturnMappedDtos() {
        when(repository.findByPackageIdOrderByOccurredAtAsc("PKG-1")).thenReturn(List.of(entry));

        List<TransitionDto> result = handler.handle(new FindHistoryQuery("PKG-1"));

        assertThat(result).hasSize(1);
        assertThat(result.get(0)).isEqualTo(
                new TransitionDto(null, "PKG-1", "LOC-A", "NEW",
                        Instant.parse("2026-08-26T10:00:00Z")));
    }

    @Test
    @DisplayName("FindTransitionByIdQuery : trouvé -> DTO")
    void byId_found_shouldReturnDto() {
        when(repository.findById(5L)).thenReturn(Optional.of(entry));

        TransitionDto dto = handler.handle(new FindTransitionByIdQuery(5L));

        assertThat(dto.packageId()).isEqualTo("PKG-1");
        assertThat(dto.status()).isEqualTo("NEW");
    }

    @Test
    @DisplayName("FindTransitionByIdQuery : inconnu -> NotFoundException")
    void byId_unknown_shouldThrowNotFound() {
        when(repository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> handler.handle(new FindTransitionByIdQuery(99L)))
                .isInstanceOf(NotFoundException.class)
                .hasMessageContaining("99");
    }
}
