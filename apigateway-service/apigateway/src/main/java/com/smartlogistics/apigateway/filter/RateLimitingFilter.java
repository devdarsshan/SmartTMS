package com.smartlogistics.apigateway.filter;

import com.smartlogistics.apigateway.config.RateLimitingConfig;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.ConsumptionProbe;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

/**
 * Global rate limiting filter for API Gateway using Bucket4j
 * Applies rate limiting to all incoming requests based on client IP
 * Rate limit: 100 requests per minute per IP
 */
@Component
public class RateLimitingFilter implements GlobalFilter, Ordered {

    @Autowired
    private RateLimitingConfig rateLimitingConfig;

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        String clientIp = getClientIp(request);
        
        Bucket bucket = rateLimitingConfig.resolveBucket(clientIp);
        ConsumptionProbe probe = bucket.tryConsumeAndReturnRemaining(1);
        
        if (probe.isConsumed()) {
            // Add rate limit headers to response
            exchange.getResponse().getHeaders().add("X-Rate-Limit-Remaining", 
                    String.valueOf(probe.getRemainingTokens()));
            return chain.filter(exchange);
        } else {
            long waitForRefill = probe.getRoundedSecondsToWait();
            ServerHttpResponse response = exchange.getResponse();
            response.setStatusCode(HttpStatus.TOO_MANY_REQUESTS);
            response.getHeaders().add("Retry-After", String.valueOf(waitForRefill));
            response.getHeaders().add("X-Rate-Limit-Remaining", "0");
            return response.setComplete();
        }
    }

    /**
     * Extract client IP from request headers
     * Considers X-Forwarded-For header for proxy scenarios
     */
    private String getClientIp(ServerHttpRequest request) {
        String xForwardedFor = request.getHeaders().getFirst("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            return xForwardedFor.split(",")[0].trim();
        }
        
        String remoteAddr = request.getRemoteAddress() != null 
                ? request.getRemoteAddress().getAddress().getHostAddress() 
                : "unknown";
        return remoteAddr;
    }

    @Override
    public int getOrder() {
        // Execute rate limiting filter before other filters (lower order = higher priority)
        return -2;
    }
}

