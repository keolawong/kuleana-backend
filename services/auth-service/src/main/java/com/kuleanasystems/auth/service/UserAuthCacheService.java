package com.kuleanasystems.auth.service;

import com.kuleanasystems.auth.config.CacheConfig;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.stereotype.Service;

import java.util.Date;

@Service
public class UserAuthCacheService {

    @Autowired
    private CacheManager cacheManager;

    public void evictUser(String username) {
        Cache cache = cacheManager.getCache(CacheConfig.USER_AUTH_CACHE);
        if (cache != null) {
            cache.put(username, new Date().getTime());
        }
    }

    public boolean isTokenStale(String username, Date tokenIssuedAt) {
        Cache cache = cacheManager.getCache(CacheConfig.USER_AUTH_CACHE);
        if (cache == null) {
            return false;
        }

        Cache.ValueWrapper valueWrapper = cache.get(username);
        if (valueWrapper != null) {
            Long lastEvictionTime = (Long) valueWrapper.get();
            return lastEvictionTime != null && tokenIssuedAt.getTime() < lastEvictionTime;
        }
        return false;
    }
}
