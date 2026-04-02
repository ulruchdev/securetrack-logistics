package com.securetrack.packageservice.domain.model;

import com.securetrack.packageservice.domain.Enum.PackageStatus;
import lombok.*;

        import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Package {

    private String packageId;
    private String description;
    private double weight;
    private boolean fragile;
    private PackageStatus status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static Package create(String description, double weight, boolean fragile) {
        return Package.builder().
                description(description)
                .weight(weight)
                .fragile(fragile)
                .status(PackageStatus.CREATED)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
    }

    public void updateStatus(PackageStatus newStatus) {
        this.status = newStatus;
        this.updatedAt = LocalDateTime.now();
    }
}