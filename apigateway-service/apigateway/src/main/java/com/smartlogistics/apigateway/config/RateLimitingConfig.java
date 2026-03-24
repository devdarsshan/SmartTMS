package com.smartlogistics.apigateway.config;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.Bucket4j;
import io.github.bucket4j.Refill;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Configuration
public class RateLimitingConfig {

    private final Map<String, Bucket> cacheBuckets = new ConcurrentHashMap<>();

    /**
     * Get or create a rate limiting bucket for a client IP
     * Rate limit: 100 requests per minute
     */
    public Bucket resolveBucket(String key) {
        return cacheBuckets.computeIfAbsent(key, k -> createNewBucket());
    }

    private Bucket createNewBucket() {
        Bandwidth limit = Bandwidth.classic(100, Refill.intervally(100, Duration.ofMinutes(1)));
        return Bucket4j.builder()
                .addLimit(limit)
                .build();
    }

    /**
     * Reset bucket for a specific key (optional, for testing/admin purposes)
     */
    public void resetBucket(String key) {
        cacheBuckets.remove(key);
    }

    /**
     * Clear all buckets (optional, for testing/admin purposes)
     */
    public void clearAllBuckets() {
        cacheBuckets.clear();
    }
}

