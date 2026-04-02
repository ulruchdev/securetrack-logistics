package com.securetrack.packageservice.presentation.dto.response;

import com.securetrack.packageservice.domain.Enum.PackageStatus;
import lombok.*;

        import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PackageResponse {
    private String packageId;
    private String description;
    private double weight;
    private boolean fragile;
    private PackageStatus status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}