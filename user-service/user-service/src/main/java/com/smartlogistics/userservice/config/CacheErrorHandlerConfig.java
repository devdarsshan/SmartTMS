package com.smartlogistics.userservice.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.Cache;
import org.springframework.cache.interceptor.CacheErrorHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class CacheErrorHandlerConfig {

    private static final Logger logger = LoggerFactory.getLogger(CacheErrorHandlerConfig.class);

    @Bean
    public CacheErrorHandler cacheErrorHandler() {
        return new CacheErrorHandler() {
            @Override
            public void handleCacheGetError(RuntimeException exception, Cache cache, Object key) {
                logAndContinue("GET", exception, cache, key);
            }

            @Override
            public void handleCachePutError(RuntimeException exception, Cache cache, Object key, Object value) {
                logAndContinue("PUT", exception, cache, key);
            }

            @Override
            public void handleCacheEvictError(RuntimeException exception, Cache cache, Object key) {
                logAndContinue("EVICT", exception, cache, key);
            }

            @Override
            public void handleCacheClearError(RuntimeException exception, Cache cache) {
                logAndContinue("CLEAR", exception, cache, "ALL");
            }

            private void logAndContinue(String operation, RuntimeException exception, Cache cache, Object key) {
                String cacheName = cache != null ? cache.getName() : "unknown";
                logger.warn(
                        "Cache {} failed for cache='{}', key='{}'. Continuing without cache. Reason: {}",
                        operation,
                        cacheName,
                        key,
                        exception.getMessage()
                );
                logger.debug("Cache {} failure details", operation, exception);
            }
        };
    }
}
