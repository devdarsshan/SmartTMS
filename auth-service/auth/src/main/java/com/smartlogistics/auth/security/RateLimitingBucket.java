package com.smartlogistics.auth.security;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.bucket4j.Bandwidth;
import com.bucket4j.Bucket;
import com.bucket4j.Bucket4j;
import com.bucket4j.Refill;

import java.time.Duration;

public class RateLimitingBucket {

    private final Cache<String, Bucket> cache = Caffeine.newBuilder()
            .expireAfterAccess(Duration.ofMinutes(10))
            .maximumSize(1000)
            .build();

    public Bucket resolveBucket(String key) {
        return cache.get(key, k -> createNewBucket());
    }

    private Bucket createNewBucket() {
        Bandwidth limit = Bandwidth.classic(
                5,
                Refill.intervally(5, Duration.ofMinutes(1))
        );

        return Bucket4j.builder()
                .addLimit(limit)
                .build();
    }
}