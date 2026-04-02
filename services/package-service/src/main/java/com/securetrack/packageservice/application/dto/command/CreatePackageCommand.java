package com.securetrack.packageservice.application.dto.command;

import lombok.*;
@Builder
public record CreatePackageCommand(
     String description,
     double weight,
     boolean fragile
){}