package com.cbs.logistics.tracking_service.query;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * Entité JPA de la PROJECTION (read-model CQRS).
 *
 * <p>Correspond 1:1 à la table tracking_history créée par Liquibase.
 * Cette entité n'est JAMAIS modifiée ni supprimée après écriture :
 * c'est un journal de lecture append-only.</p>
 */
@Entity
@Table(name = "tracking_history")
@Getter
@NoArgsConstructor
public class TrackingHistoryEntry {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long trackingId;

    private String packageId;

    private String tenantId;
    private String locationId;
    private String status;
    private java.time.Instant occurredAt;

    public TrackingHistoryEntry(String packageId, String locationId,
                                String status, java.time.Instant occurredAt) {
        this.packageId = packageId;
        this.locationId = locationId;
        this.status = status;
        this.occurredAt = occurredAt;
    }
}
