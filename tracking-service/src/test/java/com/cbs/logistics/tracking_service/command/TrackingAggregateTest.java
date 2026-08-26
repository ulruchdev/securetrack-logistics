package com.cbs.logistics.tracking_service.command;

import com.cbs.logistics.tracking_service.event.TrackingTransitionedEvent;
import com.cbs.logistics.tracking_service.exception.InvalidTransitionException;
import org.axonframework.eventhandling.EventMessage;
import org.axonframework.test.aggregate.AggregateTestFixture;
import org.hamcrest.BaseMatcher;
import org.hamcrest.Description;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.Objects;

/**
 * Tests du flux d'ÉCRITURE avec la AggregateTestFixture d'Axon.
 *
 * <p>Ce que cette fixture prouve (et qu'un simple mock ne peut pas prouver) :</p>
 * <ul>
 *   <li>une commande acceptée produit bien LE bon événement ;</li>
 *   <li>les invariants rejettent les commandes illégales SANS publier d'event ;</li>
 *   <li>l'état de l'aggregate est reconstruit par REJEU des événements passés
 *       (le paramètre given() simule l'historique de l'event store).</li>
 * </ul>
 */
class TrackingAggregateTest {

    private AggregateTestFixture<TrackingAggregate> fixture;

    @BeforeEach
    void setUp() {
        fixture = new AggregateTestFixture<>(TrackingAggregate.class);
    }

    /**
     * Matcher Hamcrest : vérifie qu'une liste contient exactement UN événement
     * dont les champs métier correspondent (l'horodatage est ignoré car
     * généré par Instant.now() dans l'aggregate).
     */
    private static BaseMatcher<List<? super EventMessage<?>>> singleTransition(String packageId, String locationId, String status) {
        return new BaseMatcher<>() {
            @Override
            public boolean matches(Object actual) {
                // La fixture fournit des EventMessage ; le métier est dans getPayload()
                Object payload = actual;
                if (actual instanceof List<?> events && events.size() == 1) {
                    payload = events.get(0);
                }
                if (payload instanceof EventMessage<?> message) {
                    payload = message.getPayload();
                }
                if (!(payload instanceof TrackingTransitionedEvent e)) {
                    return false;
                }
                return packageId.equals(e.packageId())
                        && Objects.equals(locationId, e.locationId())
                        && status.equals(e.newStatus());
            }

            @Override
            public void describeTo(Description description) {
                description.appendText("un seul TrackingTransitionedEvent(" + packageId + ", "
                        + locationId + ", " + status + ")");
            }
        };
    }

    private static TrackingTransitionedEvent event(String packageId, String locationId, String status,
                                                   String instant) {
        return new TrackingTransitionedEvent(packageId, locationId, status, Instant.parse(instant));
    }

    @Test
    @DisplayName("Première transition : crée l'aggregate et publie l'événement")
    void firstTransition_shouldCreateAggregateAndPublishEvent() {
        fixture.givenNoPriorActivity()
                .when(new RegisterTransitionCommand("PKG-123", "LOC-Lyon", "NEW"))
                .expectSuccessfulHandlerExecution()
                .expectEventsMatching(singleTransition("PKG-123", "LOC-Lyon", "NEW"));
    }

    @Test
    @DisplayName("Transition suivante : l'aggregate existant (rejoué) publie le nouvel événement")
    void subsequentTransition_shouldPublishEventFromReplayedState() {
        // given = l'historique de l'event store : le colis est passé par NEW
        fixture.given(event("PKG-123", null, "NEW", "2026-08-26T10:00:00Z"))
                .when(new RegisterTransitionCommand("PKG-123", "LOC-Paris", "IN_TRANSIT"))
                .expectSuccessfulHandlerExecution()
                .expectEventsMatching(singleTransition("PKG-123", "LOC-Paris", "IN_TRANSIT"));
    }

    @Test
    @DisplayName("INVARIANT : aucune transition après DELIVERED")
    void transitionAfterDelivered_shouldBeRejectedWithoutAnyEvent() {
        fixture.given(
                        event("PKG-999", null, "NEW", "2026-08-26T09:00:00Z"),
                        event("PKG-999", "LOC-Paris", "DELIVERED", "2026-08-26T12:00:00Z"))
                .when(new RegisterTransitionCommand("PKG-999", "LOC-Nice", "IN_TRANSIT"))
                .expectException(InvalidTransitionException.class)
                .expectNoEvents();   // preuve : rien n'a été écrit dans l'event store
    }

    @Test
    @DisplayName("Statut inconnu : rejeté sans publication")
    void unknownStatus_shouldBeRejected() {
        fixture.givenNoPriorActivity()
                .when(new RegisterTransitionCommand("PKG-42", null, "ARCHIVED"))
                .expectException(InvalidTransitionException.class)
                .expectNoEvents();
    }

    @Test
    @DisplayName("Même statut que l'actuel : autorisé (pas une violation)")
    void sameStatus_shouldBeAllowed() {
        fixture.given(event("PKG-7", "LOC-A", "IN_TRANSIT", "2026-08-26T10:00:00Z"))
                .when(new RegisterTransitionCommand("PKG-7", "LOC-B", "IN_TRANSIT"))
                .expectSuccessfulHandlerExecution();
    }
}
