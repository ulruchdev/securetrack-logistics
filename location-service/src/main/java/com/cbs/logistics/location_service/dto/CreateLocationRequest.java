package com.cbs.logistics.location_service.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateLocationRequest {

    @NotNull(message = "L'ID du package est obligatoire")
    private Long packageId;

    @NotBlank(message = "La ville est obligatoire")
    private String city;

    @NotBlank(message = "La zone est obligatoire")
    private String zone;

    @NotNull(message = "La disponibilité du checkpoint est obligatoire")
    private Boolean checkpointAvailable;
}
