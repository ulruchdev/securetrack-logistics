package com.cbs.logistics.tracking_service.listener;

import com.cbs.logistics.common.dto.PackageStatusChangedEvent;
import com.cbs.logistics.tracking_service.command.RegisterTransitionCommand;
import org.axonframework.commandhandling.gateway.CommandGateway;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.concurrent.CompletableFuture;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PackageStatusChangedListenerTest {

    @Mock
    private CommandGateway commandGateway;

    @InjectMocks
    private PackageStatusChangedListener listener;

    private PackageStatusChangedEvent event;

    @BeforeEach
    void setUp() {
        event = new PackageStatusChangedEvent(
                1L,
                "NEW",
                "IN_TRANSIT",
                10L,
                Instant.now()
        );
        when(commandGateway.sendAndWait(any())).thenReturn(CompletableFuture.completedFuture(null));
    }

    @Test
    void shouldSendCommand_WhenEventReceived() {
        // When
        listener.onStatusChanged(event);

        // Then
        ArgumentCaptor<RegisterTransitionCommand> captor = ArgumentCaptor.forClass(RegisterTransitionCommand.class);
        verify(commandGateway).sendAndWait(captor.capture());

        RegisterTransitionCommand command = captor.getValue();
        assertThat(command.packageId()).isEqualTo("1");
        assertThat(command.locationId()).isEqualTo("10");
        assertThat(command.newStatus()).isEqualTo("IN_TRANSIT");
    }

    @Test
    void shouldHandleNullLocationId() {
        // Given
        PackageStatusChangedEvent eventNoLocation = new PackageStatusChangedEvent(
                2L, null, "NEW", null, Instant.now()
        );

        // When
        listener.onStatusChanged(eventNoLocation);

        // Then
        ArgumentCaptor<RegisterTransitionCommand> captor = ArgumentCaptor.forClass(RegisterTransitionCommand.class);
        verify(commandGateway).sendAndWait(captor.capture());

        RegisterTransitionCommand command = captor.getValue();
        assertThat(command.packageId()).isEqualTo("2");
        assertThat(command.locationId()).isNull();
        assertThat(command.newStatus()).isEqualTo("NEW");
    }

    @Test
    void shouldRethrow_WhenCommandFails() {
        // Given : le gateway lève une exception (ex: 409 transition invalide)
        when(commandGateway.sendAndWait(any())).thenThrow(new RuntimeException("Transition invalide"));

        // When & Then : exception propagée pour que RabbitMQ redelivre le message (NACK)
        assertThatThrownBy(() -> listener.onStatusChanged(event))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Transition invalide");
        verify(commandGateway).sendAndWait(any());
    }
}
