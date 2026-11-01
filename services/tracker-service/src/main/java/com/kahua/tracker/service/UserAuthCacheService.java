package com.kahua.tracker.service;

import com.kahua.tracker.config.CacheConfig;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.stereotype.Service;

import java.util.Date;

@Service
public class UserAuthCacheService {

    @Autowired
    private CacheManager cacheManager;

    /**
     * Marks all existing tokens for a user as invalid by recording the current time.
     * Any token issued before this time will be rejected.
     * @param username The user to invalidate sessions for.
     */
    public void evictUser(String username) {
        Cache cache = cacheManager.getCache(CacheConfig.USER_AUTH_CACHE);
        if (cache != null) {
            // Store the timestamp (in milliseconds) when the user's auth details were changed.
            cache.put(username, new Date().getTime());
        }
    }

    /**
     * Checks if a token is still valid by comparing its issuance date
     * with the last time the user's auth details were changed.
     * @param username The user who the token belongs to.
     * @param tokenIssuedAt The 'iat' (issued at) date from the JWT.
     * @return true if the token is stale/invalid, false otherwise.
     */
    public boolean isTokenStale(String username, Date tokenIssuedAt) {
        Cache cache = cacheManager.getCache(CacheConfig.USER_AUTH_CACHE);
        if (cache == null) {
            return false;
        }

        Cache.ValueWrapper valueWrapper = cache.get(username);
        if (valueWrapper != null) {
            Long lastEvictionTime = (Long) valueWrapper.get();
            // If the token was issued *before* the last time the user was evicted, it's stale.
            return lastEvictionTime != null && tokenIssuedAt.getTime() < lastEvictionTime;
        }
        return false;
    }
}
