package com.cbs.logistics.package_service.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Entity
@Table
@AllArgsConstructor
@NoArgsConstructor
@Data
public class Package {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long packageId;

    @Column(nullable = false)
    private String tenantId;

    /** Verrou optimiste : incrémenté à chaque UPDATE, détecte les modifications concurrentes. */
    @Version
    private Long version;

    /** Tracking number unique par tenant : format ST-XXXXXXXX (8 alphanumériques). */
    @Column(nullable = false, unique = true)
    private String trackingNumber;

    @Column
    private String description;
    @Column
    private String packageName;
    @Column
    private String packageType;
    @Column
    private Double weight;
    @Column
    private boolean fragile;
    @Enumerated(EnumType.STRING)
    @Column
    private PackageStatus packageStatus;
    @Column
    private String locationId;

    /** Soft delete : timestamp de suppression logique (null = actif). */
    @Column(name = "deleted_at")
    private Instant deletedAt;
}
