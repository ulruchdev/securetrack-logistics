package com.cbs.logistics.location_service.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CreateCheckpointRequest(
    @NotNull(message = "Le siteId est obligatoire")
    Long siteId,
    @NotBlank(message = "Le nom du checkpoint est obligatoire")
    @Size(max = 255)
    String name
) {}
