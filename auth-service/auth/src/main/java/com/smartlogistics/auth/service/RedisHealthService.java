package com.smartlogistics.auth.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

/**
 * Redis Health Check Service
 * Monitors Redis connectivity and provides health status
 */
@Service
public class RedisHealthService {

    private static final Logger logger = LoggerFactory.getLogger(RedisHealthService.class);
    private final RedisTemplate<String, Object> redisTemplate;

    public RedisHealthService(RedisTemplate<String, Object> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    public boolean isRedisAvailable() {
        try {
            redisTemplate.opsForValue().set("auth:health:check", "ping");
            String result = (String) redisTemplate.opsForValue().get("auth:health:check");
            redisTemplate.delete("auth:health:check");
            
            boolean available = "ping".equals(result);
            if (available) {
                logger.debug("Redis health check passed");
            } else {
                logger.warn("Redis health check failed: unexpected result");
            }
            return available;
        } catch (Exception e) {
            logger.error("Redis health check failed: {}", e.getMessage());
            return false;
        }
    }

    public void clearUserCache(String username) {
        try {
            String userCacheKey = "auth:user:" + username;
            redisTemplate.delete(userCacheKey);
            logger.info("Cleared cache for user: {}", username);
        } catch (Exception e) {
            logger.error("Error clearing user cache: {}", e.getMessage());
        }
    }

    public void clearAllAuthCache() {
        try {
            redisTemplate.delete(redisTemplate.keys("auth:*"));
            logger.info("Cleared all auth-related cache entries");
        } catch (Exception e) {
            logger.error("Error clearing auth cache: {}", e.getMessage());
        }
    }

    public long getCacheKeyCount() {
        try {
            return redisTemplate.keys("auth:*").size();
        } catch (Exception e) {
            logger.error("Error counting cache keys: {}", e.getMessage());
            return -1;
        }
    }
}
