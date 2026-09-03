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

    @NotBlank(message = "Le tracking number du colis est obligatoire (format ST-XXXXXXXX)")
    private String trackingNumber;

    @NotNull(message = "L'ID du checkpoint est obligatoire")
    private Long checkpointId;

    private LocalDateTime checkpointTime;

    @NotNull(message = "Le résultat du contrôle est obligatoire")
    private CheckpointResult result;

    private String comment;

    // createdBy est extrait du JWT sub claim, pas du request body
}
