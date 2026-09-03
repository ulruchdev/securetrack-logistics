package com.cbs.logistics.common.security.config;

import com.cbs.logistics.common.security.filter.TenantFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Enregistre {@link TenantFilter} comme bean Spring.
 *
 * <p>Le filtre n'est pas annoté {@code @Component} pour éviter
 * une double instance (une auto-enregistrée, une ajoutée via
 * {@code addFilterBefore}). Ce {@code @Configuration} fournit
 * l'unique instance gérée par Spring.</p>
 */
@Configuration
public class TenantFilterConfig {

    @Bean
    public TenantFilter tenantFilter() {
        return new TenantFilter();
    }
}
