package com.cbs.logistics.common.security.filter;

import com.cbs.logistics.common.security.context.TenantContext;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Filtre qui extrait le {@code tenant_id} du JWT et le place
 * dans le {@link TenantContext} pour la duree de la requete.
 *
 * <p>Ce filre s'execute APRES la validation JWT par Spring Security
 * ({@code BearerTokenAuthenticationFilter}). Il lit le claim
 * {@code tenant_id} depuis le {@code Jwt} deja valide dans le
 * {@code SecurityContextHolder}.</p>
 *
 * <p>Si le claim {@code tenant_id} est absent ou vide, la requete
 * est rejetee avec un probleme RFC 7807 (400).</p>
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
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth != null && auth.getPrincipal() instanceof Jwt jwt) {
                String tenantId = jwt.getClaimAsString(CLAIM_TENANT);
                if (tenantId != null && !tenantId.isBlank()) {
                    TenantContext.setCurrent(tenantId);
                    log.debug("Tenant resolu: {}", tenantId);
                } else {
                    log.warn("JWT sans claim tenant_id - requete: {}", request.getRequestURI());
                    response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                    response.setContentType("application/problem+json");
                    response.getWriter().write("""
                            {
                              "type": "about:blank",
                              "title": "Bad Request",
                              "status": 400,
                              "detail": "Le claim tenant_id est requis dans le token JWT."
                            }
                            """);
                    return;
                }
            }
            filterChain.doFilter(request, response);
        } finally {
            TenantContext.clear();
        }
    }

    /**
     * Ne s'execute que pour les requetes authentifiees (pas health, swagger).
     */
    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        return path.startsWith("/actuator/")
                || path.startsWith("/swagger-ui")
                || path.startsWith("/v3/api-docs");
    }
}
