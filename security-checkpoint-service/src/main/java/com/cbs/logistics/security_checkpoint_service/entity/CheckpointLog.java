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
    private Long packageId;

    @Column(nullable = false)
    private String locationId;

    @Column(nullable = false)
    private LocalDateTime checkpointTime;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private CheckpointResult result;

    private String comment;

    private String createdBy;

    @PrePersist
    protected void onCreate() {
        if (checkpointTime == null) {
            checkpointTime = LocalDateTime.now();
        }
    }
}