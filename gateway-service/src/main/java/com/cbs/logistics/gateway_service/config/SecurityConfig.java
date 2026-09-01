package com.cbs.logistics.gateway_service.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.oauth2.jwt.NimbusReactiveJwtDecoder;
import org.springframework.security.oauth2.jwt.ReactiveJwtDecoder;
import org.springframework.security.oauth2.jwt.ReactiveJwtDecoders;
import org.springframework.security.web.server.SecurityWebFilterChain;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.util.Base64;

/**
 * Sécurité du Gateway — filtre JWT sur toutes les routes.
 *
 * <p>Swagger UI est accessible sans token (via /service/**).
 * Toutes les routes API (/api/**) nécessitent un JWT valide.</p>
 */
@Configuration
@EnableWebFluxSecurity
public class SecurityConfig {

    @Value("${jwt.jwk-set-uri:}")
    private String jwkSetUri;

    @Value("${jwt.secret:}")
    private String secret;

    @Bean
    @Order(Ordered.HIGHEST_PRECEDENCE)
    public SecurityWebFilterChain securityWebFilterChain(ServerHttpSecurity http) {
        http
            .csrf(ServerHttpSecurity.CsrfSpec::disable)
            .authorizeExchange(exchanges -> exchanges
                // Actuator — toujours accessible
                .pathMatchers("/actuator/**").permitAll()

                // Swagger UI / API docs — accessible sans JWT
                .pathMatchers("/service/package/swagger-ui/**").permitAll()
                .pathMatchers("/service/package/v3/api-docs/**").permitAll()
                .pathMatchers("/service/location/swagger-ui/**").permitAll()
                .pathMatchers("/service/location/v3/api-docs/**").permitAll()
                .pathMatchers("/service/checkpoint/swagger-ui/**").permitAll()
                .pathMatchers("/service/checkpoint/v3/api-docs/**").permitAll()
                .pathMatchers("/service/tracking/swagger-ui/**").permitAll()
                .pathMatchers("/service/tracking/v3/api-docs/**").permitAll()

                // Tout le reste : JWT requis
                .anyExchange().authenticated()
            )
            .oauth2ResourceServer(oauth2 -> oauth2
                .jwt(jwt -> jwt.jwtDecoder(reactiveJwtDecoder()))
            );

        return http.build();
    }

    @Bean
    public ReactiveJwtDecoder reactiveJwtDecoder() {
        if (jwkSetUri != null && !jwkSetUri.isBlank()) {
            return ReactiveJwtDecoders.fromIssuerLocation(jwkSetUri);
        }
        if (secret != null && !secret.isBlank()) {
            byte[] keyBytes = Base64.getEncoder().encode(secret.getBytes());
            SecretKey key = new SecretKeySpec(keyBytes, "HMACSHA256");
            return NimbusReactiveJwtDecoder.withSecretKey(key).build();
        }
        throw new IllegalStateException(
                "Aucune clé JWT configurée. Définissez jwt.secret ou jwt.jwk-set-uri.");
    }
}
