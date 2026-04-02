package com.securetrack.packageservice.application.dto.command;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.*;

@Builder
public record CreatePackageCommand(
        @NotBlank String description,
        @NotNull @Positive double weight,
        boolean fragile
) {}