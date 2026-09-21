package com.wrapitup.wrap.cache;

import com.wrapitup.common.dto.WrapResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

/**
 * Service for caching completed Wrap results in Redis.
 *
 * Cache Key Format: wrap:{spotifyAccountId}
 * Cache Value: Complete WrapResponse DTO
 * Default TTL: 15 minutes (configurable)
 *
 * Used to prevent redundant Spotify API calls and analysis re-computation
 * for the same user within the TTL window.
 */
@Slf4j
@Service
public class WrapCacheService {

    private final RedisTemplate<String, WrapResponse> redisTemplate;
    private final long ttlMinutes;

    public WrapCacheService(
            RedisTemplate<String, WrapResponse> redisTemplate,
            @Value("${wrap.cache.ttl-minutes:15}") long ttlMinutes
    ) {
        this.redisTemplate = redisTemplate;
        this.ttlMinutes = ttlMinutes;
    }

    /**
     * Save a completed Wrap result to Redis cache.
     *
     * @param spotifyAccountId Spotify user ID (used as cache key)
     * @param wrap Complete WrapResponse to cache
     */
    public void save(String spotifyAccountId, WrapResponse wrap) {
        try {
            String cacheKey = buildCacheKey(spotifyAccountId);
            redisTemplate.opsForValue().set(cacheKey, wrap, ttlMinutes, TimeUnit.MINUTES);
            log.debug("Cached wrap for user: accountId={}, ttl={}min", spotifyAccountId, ttlMinutes);
        } catch (Exception e) {
            log.warn("Failed to cache wrap for user: accountId={}, error={}", spotifyAccountId, e.getMessage());
            // Don't throw; caching failure shouldn't break functionality
        }
    }

    /**
     * Retrieve a cached Wrap result from Redis.
     *
     * @param spotifyAccountId Spotify user ID
     * @return WrapResponse if cached and valid, null otherwise
     */
    public WrapResponse get(String spotifyAccountId) {
        try {
            String cacheKey = buildCacheKey(spotifyAccountId);
            WrapResponse cached = redisTemplate.opsForValue().get(cacheKey);
            if (cached != null) {
                log.debug("Cache hit for user: accountId={}", spotifyAccountId);
            } else {
                log.debug("Cache miss for user: accountId={}", spotifyAccountId);
            }
            return cached;
        } catch (Exception e) {
            log.warn("Failed to retrieve cached wrap for user: accountId={}, error={}", spotifyAccountId, e.getMessage());
            return null;
        }
    }

    /**
     * Delete a cached Wrap result from Redis.
     *
     * @param spotifyAccountId Spotify user ID
     */
    public void delete(String spotifyAccountId) {
        try {
            String cacheKey = buildCacheKey(spotifyAccountId);
            Boolean deleted = redisTemplate.delete(cacheKey);
            if (Boolean.TRUE.equals(deleted)) {
                log.debug("Deleted cached wrap for user: accountId={}", spotifyAccountId);
            }
        } catch (Exception e) {
            log.warn("Failed to delete cached wrap for user: accountId={}, error={}", spotifyAccountId, e.getMessage());
        }
    }

    /**
     * Check if a Wrap result is cached for the given user.
     *
     * @param spotifyAccountId Spotify user ID
     * @return true if cached, false otherwise
     */
    public boolean exists(String spotifyAccountId) {
        try {
            String cacheKey = buildCacheKey(spotifyAccountId);
            Boolean exists = redisTemplate.hasKey(cacheKey);
            return Boolean.TRUE.equals(exists);
        } catch (Exception e) {
            log.warn("Failed to check cache existence for user: accountId={}, error={}", spotifyAccountId, e.getMessage());
            return false;
        }
    }

    /**
     * Build the Redis cache key for a given Spotify user.
     *
     * @param spotifyAccountId Spotify user ID
     * @return Cache key in format "wrap:{spotifyAccountId}"
     */
    private String buildCacheKey(String spotifyAccountId) {
        return "wrap:" + spotifyAccountId;
    }
}