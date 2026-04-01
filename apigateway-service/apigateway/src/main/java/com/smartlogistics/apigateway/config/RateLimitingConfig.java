package com.smartlogistics.apigateway.config;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.Bucket4j;
import io.github.bucket4j.Refill;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Rate Limiting Configuration for API Gateway
 * Uses in-memory rate limiting
 */
@Configuration
public class RateLimitingConfig {

    private static final Logger logger = LoggerFactory.getLogger(RateLimitingConfig.class);

    // In-memory cache
    private final Map<String, Bucket> cacheBuckets = new ConcurrentHashMap<>();

    @Value("${rate.limiting.requests.per.minute:100}")
    private int requestsPerMinute;

    public Bucket resolveBucket(String key) {
        logger.debug("Using in-memory rate limiting for key: {}", key);
        return cacheBuckets.computeIfAbsent(key, k -> createNewBucket());
    }

    private Bucket createNewBucket() {
        Bandwidth limit = Bandwidth.classic(requestsPerMinute,
                Refill.intervally(requestsPerMinute, Duration.ofMinutes(1)));
        return Bucket4j.builder()
                .addLimit(limit)
                .build();
    }

    public void resetBucket(String key) {
        cacheBuckets.remove(key);
        logger.debug("Reset rate limit for key: {}", key);
    }

    public void clearAllBuckets() {
        cacheBuckets.clear();
        logger.debug("Cleared all in-memory rate limit buckets");
    }
}
