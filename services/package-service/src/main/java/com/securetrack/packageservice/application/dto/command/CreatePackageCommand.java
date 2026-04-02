package com.securetrack.packageservice.application.dto.command;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import lombok.*;

@Builder
public record CreatePackageCommand(
        @NotBlank String description,
        @Positive double weight,
        boolean fragile
) {}