package com.cbs.logistics.security_checkpoint_service.dto;

import com.cbs.logistics.security_checkpoint_service.entity.CheckpointResult;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateCheckpointRequest {

    @NotNull(message = "L'ID du colis est obligatoire")
    private Long packageId;

    @NotBlank(message = "L'ID de la localisation est obligatoire")
    private String locationId;

    private LocalDateTime checkpointTime;

    @NotNull(message = "Le résultat du contrôle est obligatoire")
    private CheckpointResult result;

    private String comment;

    @NotBlank(message = "L'agent créateur est obligatoire")
    private String createdBy;
}