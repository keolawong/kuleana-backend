package com.kuleanasystems.auth.service;

import com.kuleanasystems.auth.config.CacheConfig;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.stereotype.Service;

@Service
public class TokenBlacklistService {

    @Autowired
    private CacheManager cacheManager;

    public boolean isBlacklisted(String token) {
        Cache cache = cacheManager.getCache(CacheConfig.TOKEN_CACHE);
        if (cache != null && cache.get(token) != null) {
            return true;
        }
        return false;
    }

    public void blacklist(String token) {
        Cache cache = cacheManager.getCache(CacheConfig.TOKEN_CACHE);
        if (cache != null) {
            cache.put(token, true);
        }
    }
}
