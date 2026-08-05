package com.cbs.logistics.location_service.config;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Info;
import org.springframework.context.annotation.Configuration;

@Configuration
@OpenAPIDefinition(
        info = @Info(
                title = "Location Service API",
                version = "1.0.0",
                description = "API pour la gestion des emplacements logistiques"
        )
)
public class OpenApiConfig {
}