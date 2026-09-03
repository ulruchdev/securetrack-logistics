package com.cbs.logistics.security_checkpoint_service.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "checkpoint_logs")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CheckpointLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String tenantId;

    /** Numéro de suivi du colis (format ST-XXXXXXXX), lookup via Package Service. */
    @Column(nullable = false)
    private String trackingNumber;

    /** ID du checkpoint (point de contrôle physique) dans le Location Service. */
    @Column(nullable = false)
    private Long checkpointId;

    @Column(nullable = false)
    private LocalDateTime checkpointTime;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private CheckpointResult result;

    private String comment;

    /** Identifiant de l'agent (extrait du JWT sub claim). */
    @Column(nullable = false)
    private String createdBy;

    @PrePersist
    protected void onCreate() {
        if (checkpointTime == null) {
            checkpointTime = LocalDateTime.now();
        }
    }
}
