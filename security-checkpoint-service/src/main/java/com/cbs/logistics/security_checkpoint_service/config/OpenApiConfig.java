package com.cbs.logistics.security_checkpoint_service.config;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import org.springframework.context.annotation.Configuration;

@Configuration
@SecurityScheme(
        name = "basicAuth",
        type = SecuritySchemeType.HTTP,
        scheme = "basic",
        description = "Authentification Basic HTTP (admin / SECURITY_PASSWORD)"
)
@SecurityRequirement(name = "basicAuth")
@OpenAPIDefinition(
        info = @Info(
                title = "Security Checkpoint Service API",
                version = "1.0.0",
                description = "API pour la gestion des logs de passage aux checkpoints de securite"
        )
)
public class OpenApiConfig {
}
