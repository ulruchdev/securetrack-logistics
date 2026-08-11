package com.cbs.logistics.package_service.config;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Info;
import org.springframework.context.annotation.Configuration;

@Configuration
@OpenAPIDefinition(
        info = @Info(
                title = "Package Service API",
                version = "1.0.0",
                description = "API pour la gestion des colis"
        )
)
public class OpenApiConfig {
}