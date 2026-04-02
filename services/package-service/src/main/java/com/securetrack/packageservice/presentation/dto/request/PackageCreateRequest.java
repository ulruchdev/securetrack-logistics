package com.securetrack.packageservice.presentation.dto.request;

import jakarta.validation.constraints.*;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PackageCreateRequest {

    @NotBlank(message = "La description est obligatoire")
    @Size(max = 500, message = "La description ne doit pas dépasser 500 caractères")
    private String description;

    @Positive(message = "Le poids doit être supérieur à 0")
    @DecimalMax(value = "500.00", message = "Le poids maximum est 500 kg")
    private double weight;

    private boolean fragile;
}