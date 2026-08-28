package com.cbs.logistics.tracking_service.listener;

import com.cbs.logistics.common.dto.PackageStatusChangedEvent;
import com.cbs.logistics.tracking_service.command.RegisterTransitionCommand;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.axonframework.commandhandling.gateway.CommandGateway;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

/**
 * Consommateur des événements PackageStatusChangedEvent publiés
 * par Package Service via RabbitMQ.
 *
 * <p>Chaque événement reçu est converti en RegisterTransitionCommand
 * et envoyé au CommandGateway Axon pour enregistrement dans l'event store.</p>
 *
 * <p>L'exchange est "package-status" (topic), routing key "status.changed".
 * La queue "tracking-service.package-status" est créée automatiquement
 * par RabbitAdmin à partir de la déclaration @Queue dans la config RabbitMQ.</p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PackageStatusChangedListener {

    private final CommandGateway commandGateway;

    /**
     * Écoute la queue "tracking-service.package-status" liée à l'exchange
     * "package-status" avec la routing key "status.changed".
     *
     * @param event l'événement de changement de statut
     */
    @RabbitListener(
            queues = "tracking-service.package-status",
            containerFactory = "rabbitListenerContainerFactory"
    )
    public void onStatusChanged(PackageStatusChangedEvent event) {
        log.info("Received status change: package {} {} -> {}",
                event.packageId(), event.previousStatus(), event.newStatus());

        try {
            RegisterTransitionCommand command = new RegisterTransitionCommand(
                    String.valueOf(event.packageId()),
                    event.locationId() != null ? String.valueOf(event.locationId()) : null,
                    event.newStatus()
            );
            commandGateway.sendAndWait(command);
            log.info("Transition registered for package {}", event.packageId());
        } catch (Exception e) {
            log.error("Failed to register transition for package {}: {}",
                    event.packageId(), e.getMessage(), e);
            // Rethrow pour que RabbitMQ redelivre le message (NACK)
            // Sinon le message est ACK et la transition est perdue
            throw e;
        }
    }
}
