package com.smartlogistics.auth.controller;

import com.smartlogistics.auth.service.RedisHealthService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * Redis Management Controller for Auth Service
 * Provides endpoints to monitor and manage Redis cache
 */
@RestController
@RequestMapping("/auth/cache")
public class RedisCacheController {

    private final RedisHealthService redisHealthService;

    public RedisCacheController(RedisHealthService redisHealthService) {
        this.redisHealthService = redisHealthService;
    }

    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> getRedisHealth() {
        Map<String, Object> health = new HashMap<>();
        health.put("redis_available", redisHealthService.isRedisAvailable());
        health.put("cache_key_count", redisHealthService.getCacheKeyCount());
        health.put("timestamp", System.currentTimeMillis());
        return ResponseEntity.ok(health);
    }

    @DeleteMapping("/user/{username}")
    public ResponseEntity<String> clearUserCache(@PathVariable String username) {
        redisHealthService.clearUserCache(username);
        return ResponseEntity.ok("Cache cleared for user: " + username);
    }

    @DeleteMapping("/all")
    public ResponseEntity<String> clearAllCache() {
        redisHealthService.clearAllAuthCache();
        return ResponseEntity.ok("All auth cache cleared");
    }
}
