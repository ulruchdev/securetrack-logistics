package com.cbs.logistics.security_checkpoint_service.config;

import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;

import java.time.Duration;

/**
 * Configuration du cache Redis pour la disponibilité des lieux.
 * TTL court (5 min) : un lieu peut devenir indisponible ; on ne veut pas
 * servir une réponse périmée trop longtemps.
 */
@Configuration
@EnableCaching
public class RedisCacheConfig {

    public static final String LOCATION_AVAILABILITY_CACHE = "locationAvailability";

    @Bean
    public CacheManager cacheManager(RedisConnectionFactory connectionFactory) {
        RedisCacheConfiguration defaultConfig = RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(Duration.ofMinutes(5))
                .disableCachingNullValues();

        return RedisCacheManager.builder(connectionFactory)
                .cacheDefaults(defaultConfig)
                .build();
    }
}
