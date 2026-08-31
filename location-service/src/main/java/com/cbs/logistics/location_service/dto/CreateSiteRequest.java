package com.cbs.logistics.location_service.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateSiteRequest(
    @NotBlank(message = "Le nom du site est obligatoire")
    @Size(max = 255)
    String name,
    String address,
    Double latitude,
    Double longitude
) {}
