package com.securetrack.packageservice.infrastructure.persistence.entity;

import com.securetrack.packageservice.domain.Enum.PackageStatus;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "packages")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class JpaPackageEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String packageId;

    @Column(nullable = false, length = 500)
    private String description;

    @Column(nullable = false, columnDefinition = "DECIMAL(6,2)")
    private double weight;

    @Column(nullable = false)
    private boolean fragile;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PackageStatus status;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private LocalDateTime updatedAt;
}