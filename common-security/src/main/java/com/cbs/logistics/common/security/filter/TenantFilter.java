package com.cbs.logistics.common.security.filter;

import com.cbs.logistics.common.security.context.TenantContext;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Base64;

/**
 * Filtre qui extrait le {@code tenant_id} du JWT et le place
 * dans le {@link TenantContext} pour la durée de la requête.
 *
 * <p>Lit le header {@code Authorization: Bearer <token>} et décode
 * le payload du JWT directement (sans vérifier la signature, car
 * {@code BearerTokenAuthenticationFilter} l'a déjà fait en amont).</p>
 */
public class TenantFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(TenantFilter.class);
    private static final String CLAIM_TENANT = "tenant_id";

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain)
            throws ServletException, IOException {

        try {
            String authHeader = request.getHeader("Authorization");
            if (authHeader != null && authHeader.startsWith("Bearer ")) {
                String token = authHeader.substring(7);
                String tenantId = extractTenantFromJwt(token);
                if (tenantId != null && !tenantId.isBlank()) {
                    TenantContext.setCurrent(tenantId);
                    log.debug("Tenant résolu: {}", tenantId);
                }
            }
            filterChain.doFilter(request, response);
        } finally {
            TenantContext.clear();
        }
    }

    /**
     * Extrait le claim {@code tenant_id} du payload JWT sans vérification
     * de signature (déjà effectuée par Spring Security en amont).
     */
    private String extractTenantFromJwt(String token) {
        try {
            String[] parts = token.split("\\.");
            if (parts.length < 2) return null;

            // Décoder le payload (partie base64url)
            String payloadB64 = parts[1];
            // Ajouter le padding manquant
            int padding = 4 - (payloadB64.length() % 4);
            if (padding != 4) {
                payloadB64 += "=".repeat(padding);
            }
            byte[] decoded = Base64.getUrlDecoder().decode(payloadB64);
            String payload = new String(decoded);

            // Extraction simple du claim tenant_id via recherche de chaîne
            // (évite une dépendance Jackson pour un seul champ)
            int idx = payload.indexOf("\"" + CLAIM_TENANT + "\"");
            if (idx < 0) return null;

            // Trouver le début de la valeur (après le ':')
            int colonIdx = payload.indexOf(':', idx);
            if (colonIdx < 0) return null;

            int start = payload.indexOf('"', colonIdx + 1);
            if (start < 0) return null;

            int end = payload.indexOf('"', start + 1);
            if (end < 0) return null;

            return payload.substring(start + 1, end);
        } catch (Exception e) {
            log.warn("Impossible d'extraire tenant_id du JWT: {}", e.getMessage());
            return null;
        }
    }

    /**
     * Ne s'exécute que pour les requêtes authentifiées (pas health, swagger).
     */
    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        return path.startsWith("/actuator/")
                || path.startsWith("/swagger-ui")
                || path.startsWith("/v3/api-docs");
    }
}
