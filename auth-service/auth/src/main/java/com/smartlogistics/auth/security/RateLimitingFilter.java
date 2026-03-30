package com.smartlogistics.auth.security;

import com.smartlogistics.auth.exception.RateLimitExceededException;
import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

/**
 * Redis-based Rate Limiting Filter
 * Provides distributed rate limiting across multiple auth-service instances
 */
@Component
public class RateLimitingFilter implements Filter {

    private static final Logger logger = LoggerFactory.getLogger(RateLimitingFilter.class);
    private static final String RATE_LIMIT_PREFIX = "auth:rate_limit:";
    private static final int MAX_REQUESTS_PER_MINUTE = 60; // 60 requests per minute per IP
    
    private final RedisTemplate<String, Object> redisTemplate;

    public RateLimitingFilter(RedisTemplate<String, Object> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        
        HttpServletRequest httpRequest = (HttpServletRequest) request;
        String clientIp = getClientIp(httpRequest);
        
        // Only apply rate limiting to auth endpoints
        String requestUri = httpRequest.getRequestURI();
        if (requestUri.contains("/auth/")) {
            if (isAllowed(clientIp)) {
                chain.doFilter(request, response);
            } else {
                logger.warn("Rate limit exceeded for IP: {} on endpoint: {}", clientIp, requestUri);
                throw new RateLimitExceededException("Too many requests. Please try again later.");
            }
        } else {
            chain.doFilter(request, response);
        }
    }

    private boolean isAllowed(String clientIp) {
        try {
            String key = RATE_LIMIT_PREFIX + clientIp + ":" + getCurrentMinute();
            
            // Get current count for this minute
            Integer currentCount = (Integer) redisTemplate.opsForValue().get(key);
            if (currentCount == null) {
                currentCount = 0;
            }
            
            if (currentCount >= MAX_REQUESTS_PER_MINUTE) {
                return false;
            }
            
            // Increment counter and set expiry
            redisTemplate.opsForValue().increment(key);
            redisTemplate.expire(key, 1, TimeUnit.MINUTES);
            
            logger.debug("Rate limit check for IP {}: {}/{}", clientIp, currentCount + 1, MAX_REQUESTS_PER_MINUTE);
            return true;
            
        } catch (Exception e) {
            logger.error("Rate limiting check failed for IP {}: {}", clientIp, e.getMessage());
            // Allow request on Redis failure to prevent service disruption
            return true;
        }
    }
    
    private long getCurrentMinute() {
        return System.currentTimeMillis() / (60 * 1000);
    }

    private String getClientIp(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            return xForwardedFor.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}

