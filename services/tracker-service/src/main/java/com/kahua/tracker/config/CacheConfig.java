package com.kahua.tracker.config;

import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.TimeUnit;

@Configuration
@EnableCaching
public class CacheConfig {

    public static final String USER_AUTH_CACHE = "userAuthEvictions";
    public static final String TOKEN_CACHE = "revokedTokens"; // Keeping this for the logout endpoint
    public static final String APPLICATION_CACHE = "applications";
    public static final String FLOW_CACHE = "flows";
    public static final String FLOW_STEP_CACHE = "flowSteps";

    @Bean
    public CacheManager cacheManager() {
        CaffeineCacheManager cacheManager = new CaffeineCacheManager(
                TOKEN_CACHE, USER_AUTH_CACHE, APPLICATION_CACHE, FLOW_CACHE, FLOW_STEP_CACHE);
        cacheManager.setCaffeine(Caffeine.newBuilder()
                .expireAfterWrite(24, TimeUnit.HOURS) // Should match JWT expiration
                .maximumSize(10_000));
        return cacheManager;
    }
}
