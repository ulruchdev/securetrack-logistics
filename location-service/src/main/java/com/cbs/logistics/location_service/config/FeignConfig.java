package com.cbs.logistics.location_service.config;

import feign.Retryer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Politique de retry Feign : absorbe les erreurs réseau transitoires
 * (connexion refusée, timeout) sur les appels inter-services idempotents (GET).
 * Backoff exponentiel 100 ms → 1 s, 3 tentatives maximum.
 */
@Configuration
public class FeignConfig {

    @Bean
    public Retryer feignRetryer() {
        return new Retryer.Default(100L, 1000L, 3);
    }
}
