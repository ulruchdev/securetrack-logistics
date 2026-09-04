package com.cbs.logistics.package_service.service;

import com.cbs.logistics.common.dto.PackageStatusChangedEvent;
import com.cbs.logistics.common.security.context.TenantContext;
import com.cbs.logistics.package_service.entity.EventOutbox;
import com.cbs.logistics.package_service.repository.EventOutboxRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

/**
 * Service de transactional outbox pour les événements RabbitMQ.
 *
 * <p>Les événements sont stockés dans la table event_outbox
 * dans la MÊME transaction que la modification de données.
 * Un scheduler poll périodiquement les événements PENDING
 * et les publie vers RabbitMQ.</p>
 *
 * <p>Avantage : en cas de panne RabbitMQ, les événements ne sont
 * pas perdus (contrairement au log.warn précédent).</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class EventOutboxService {

    private final EventOutboxRepository outboxRepository;
    private final RabbitTemplate rabbitTemplate;
    private final ObjectMapper objectMapper;

    private static final int MAX_RETRIES = 5;

    /**
     * Stocke un événement dans l'outbox (dans la même transaction que l'appelant).
     */
    @Transactional
    public void storeEvent(String eventType, String routingKey, Object payload) {
        try {
            String json = objectMapper.writeValueAsString(payload);
            EventOutbox event = EventOutbox.builder()
                    .eventType(eventType)
                    .routingKey(routingKey)
                    .payload(json)
                    .status("PENDING")
                    .createdAt(Instant.now())
                    .retryCount(0)
                    .tenantId(TenantContext.getCurrent())
                    .build();
            outboxRepository.save(event);
            log.debug("Event stored in outbox: {} on {}", eventType, routingKey);
        } catch (Exception e) {
            log.error("Failed to store event in outbox: {}", e.getMessage(), e);
            // L'événement n'est pas critique pour l'opération métier principale
        }
    }

    /**
     * Stocke un PackageStatusChangedEvent dans l'outbox.
     */
    @Transactional
    public void storePackageStatusChanged(PackageStatusChangedEvent event) {
        storeEvent("PackageStatusChanged", "status.changed", event);
    }

    /**
     * Poll l'outbox et publie les événements PENDING vers RabbitMQ.
     * Exécuté toutes les 5 secondes.
     */
    @Scheduled(fixedDelay = 5000)
    @Transactional
    public void publishPendingEvents() {
        List<EventOutbox> pending = outboxRepository.findByStatusOrderByCreatedAtAsc("PENDING");

        for (EventOutbox event : pending) {
            try {
                rabbitTemplate.convertAndSend("package-status", event.getRoutingKey(), event.getPayload());
                outboxRepository.markAsPublished(event.getId());
                log.info("Event published from outbox: {} (id={})", event.getEventType(), event.getId());
            } catch (Exception e) {
                log.warn("Failed to publish event {} (id={}, attempt {}): {}",
                        event.getEventType(), event.getId(), event.getRetryCount() + 1, e.getMessage());
                outboxRepository.markAsFailed(event.getId());

                if (event.getRetryCount() + 1 >= MAX_RETRIES) {
                    log.error("Event {} (id={}) exceeded max retries — manual intervention required",
                            event.getEventType(), event.getId());
                }
            }
        }
    }
}
