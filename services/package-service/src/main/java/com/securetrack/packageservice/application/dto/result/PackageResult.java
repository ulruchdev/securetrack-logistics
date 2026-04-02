package com.securetrack.packageservice.application.dto.result;

import com.securetrack.packageservice.domain.Enum.PackageStatus;
import lombok.*;
import java.time.LocalDateTime;
@Builder
public record PackageResult (String packageId,
    String description,
    double weight,
    boolean fragile,
    PackageStatus status,
    LocalDateTime createdAt,
    LocalDateTime updatedAt){}