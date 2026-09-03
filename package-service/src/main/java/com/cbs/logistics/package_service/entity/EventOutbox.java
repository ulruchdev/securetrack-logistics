package com.cbs.logistics.package_service.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

/**
 * Transactional Outbox : événement stocké dans la même transaction
 * que la modification de données, puis publié asynchronément vers RabbitMQ.
 *
 * <p>Pattern recommandé pour éviter la perte d'événements en cas de
 * panne du broker (au lieu du log.warn précédent).</p>
 */
@Entity
@Table(name = "event_outbox")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EventOutbox {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String eventType;

    @Column(nullable = false)
    private String routingKey;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String payload;

    @Column(nullable = false)
    private String status; // PENDING, PUBLISHED, FAILED

    @Column(nullable = false)
    private Instant createdAt;

    private Instant publishedAt;

    @Column(nullable = false)
    private int retryCount;

    @Column(nullable = false)
    private String tenantId;
}
