package com.smartlogistics.apigateway.config;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.Bucket4j;
import io.github.bucket4j.Refill;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.RedisTemplate;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

/**
 * Rate Limiting Configuration for API Gateway
 * Uses Redis for distributed rate limiting across multiple gateway instances
 * Fallback to in-memory cache if Redis is unavailable
 */
@Configuration
public class RateLimitingConfig {

    private static final Logger logger = LoggerFactory.getLogger(RateLimitingConfig.class);
    private static final String RATE_LIMIT_PREFIX = "gateway:rate_limit:";
    
    // In-memory fallback cache
    private final Map<String, Bucket> cacheBuckets = new ConcurrentHashMap<>();

    @Value("${rate.limiting.requests.per.minute:100}")
    private int requestsPerMinute;

    @Autowired(required = false)
    private RedisTemplate<String, Object> redisTemplate;

    /**
     * Get or create a rate limiting bucket for a client IP
     * Uses Redis-backed distributed rate limiting if available
     * Rate limit: 100 requests per minute (configurable)
     */
    public Bucket resolveBucket(String key) {
        // Try Redis-backed rate limiting first
        if (redisTemplate != null && isRedisAvailable()) {
            return resolveRedisBackedBucket(key);
        }
        
        // Fallback to in-memory cache
        logger.debug("Using in-memory rate limiting for key: {}", key);
        return cacheBuckets.computeIfAbsent(key, k -> createNewBucket());
    }

    /**
     * Redis-backed rate limiting bucket
     * Stores request count in Redis with 1 minute TTL
     */
    private Bucket resolveRedisBackedBucket(String key) {
        try {
            String redisKey = RATE_LIMIT_PREFIX + key + ":" + getCurrentMinute();
            
            // Get current request count from Redis
            Integer requestCount = (Integer) redisTemplate.opsForValue().get(redisKey);
            
            if (requestCount == null) {
                // First request in this minute
                redisTemplate.opsForValue().set(redisKey, 1, 1, TimeUnit.MINUTES);
                logger.debug("Redis rate limit initialized for key: {} (1/{})", key, requestsPerMinute);
            } else {
                // Increment request count
                redisTemplate.opsForValue().increment(redisKey);
                logger.debug("Redis rate limit updated for key: {} ({}/{})", key, requestCount + 1, requestsPerMinute);
            }
            
            // Create bucket based on remaining capacity
            int remainingCapacity = Math.max(0, requestsPerMinute - (requestCount == null ? 0 : requestCount));
            return createBucketWithCapacity(remainingCapacity);
            
        } catch (Exception e) {
            logger.warn("Redis rate limiting failed, falling back to in-memory: {}", e.getMessage());
            return cacheBuckets.computeIfAbsent(key, k -> createNewBucket());
        }
    }

    /**
     * Create a new bucket with default capacity
     */
    private Bucket createNewBucket() {
        Bandwidth limit = Bandwidth.classic(requestsPerMinute, 
                Refill.intervally(requestsPerMinute, Duration.ofMinutes(1)));
        return Bucket4j.builder()
                .addLimit(limit)
                .build();
    }

    /**
     * Create bucket with specific remaining capacity
     */
    private Bucket createBucketWithCapacity(int capacity) {
        Bandwidth limit = Bandwidth.classic(capacity, 
                Refill.intervally(requestsPerMinute, Duration.ofMinutes(1)));
        return Bucket4j.builder()
                .addLimit(limit)
                .build();
    }

    /**
     * Check if Redis is available
     */
    private boolean isRedisAvailable() {
        try {
            redisTemplate.getConnectionFactory().getConnection().ping();
            return true;
        } catch (Exception e) {
            logger.debug("Redis not available: {}", e.getMessage());
            return false;
        }
    }

    /**
     * Get current minute timestamp for Redis key
     */
    private long getCurrentMinute() {
        return System.currentTimeMillis() / 60000; // Convert to minutes
    }

    /**
     * Reset bucket for a specific key (optional, for testing/admin purposes)
     */
    public void resetBucket(String key) {
        cacheBuckets.remove(key);
        if (redisTemplate != null) {
            try {
                String redisKey = RATE_LIMIT_PREFIX + key + ":*";
                redisTemplate.delete(redisKey);
                logger.debug("Reset rate limit for key: {}", key);
            } catch (Exception e) {
                logger.warn("Failed to reset Redis rate limit: {}", e.getMessage());
            }
        }
    }

    /**
     * Clear all buckets (optional, for testing/admin purposes)
     */
    public void clearAllBuckets() {
        cacheBuckets.clear();
        logger.debug("Cleared all in-memory rate limit buckets");
    }
}

