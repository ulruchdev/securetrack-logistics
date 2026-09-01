package com.cbs.logistics.common.security.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.util.Base64;

/**
 * Configuration JWT Resource Server partagée.
 *
 * <p>Chaque service importe ce module et hérite de la chaîne de sécurité
 * JWT sans duplication. Le decoder est configuré via :</p>
 * <ul>
 *   <li>{@code jwt.jwk-set-uri} — URI du JWKS (prod avec Keycloak/Auth0)</li>
 *   <li>{@code jwt.secret} — clé symétrique base64 (dev/local uniquement)</li>
 * </ul>
 */
@Configuration
public class JwtConfig {

    @Value("${jwt.jwk-set-uri:}")
    private String jwkSetUri;

    @Value("${jwt.secret:}")
    private String secret;

    @Bean
    public JwtDecoder jwtDecoder() {
        if (jwkSetUri != null && !jwkSetUri.isBlank()) {
            return NimbusJwtDecoder.withJwkSetUri(jwkSetUri).build();
        }
        if (secret != null && !secret.isBlank()) {
            // Clé symétrique HMACSHA256 en base64
            byte[] keyBytes = Base64.getEncoder().encode(secret.getBytes());
            SecretKey key = new SecretKeySpec(keyBytes, "HMACSHA256");
            return NimbusJwtDecoder.withSecretKey(key).build();
        }
        throw new IllegalStateException(
                "Aucune clé JWT configurée. Définissez jwt.secret ou jwt.jwk-set-uri.");
    }
}
