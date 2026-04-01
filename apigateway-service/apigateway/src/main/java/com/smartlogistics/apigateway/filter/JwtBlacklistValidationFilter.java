package com.smartlogistics.apigateway.filter;

import com.smartlogistics.apigateway.service.JwtValidationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

/**
 * JWT Blacklist Validation Filter
 * Validates JWT tokens against Redis blacklist before allowing requests through
 * This prevents revoked tokens from accessing protected endpoints
 * 
 * Filter Order: -3 (executes before rate limiting at -2 and user propagation at -1)
 */
@Component
public class JwtBlacklistValidationFilter implements GlobalFilter, Ordered {

    private static final Logger logger = LoggerFactory.getLogger(JwtBlacklistValidationFilter.class);
    private static final String BEARER_PREFIX = "Bearer ";

    @Autowired
    private JwtValidationService jwtValidationService;

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        String path = request.getPath().value();

        // Skip JWT validation for public endpoints (auth endpoints)
        if (isPublicEndpoint(path)) {
            logger.debug("Skipping JWT validation for public endpoint: {}", path);
            return chain.filter(exchange);
        }

        // Extract JWT token from Authorization header
        String authHeader = request.getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
        
        if (authHeader == null || !authHeader.startsWith(BEARER_PREFIX)) {
            logger.debug("No Bearer token found for path: {}", path);
            // Let Spring Security handle missing token
            return chain.filter(exchange);
        }

        String token = authHeader.substring(BEARER_PREFIX.length());

        // Validate token against blacklist
        if (!jwtValidationService.isTokenValid(token)) {
            logger.warn("Invalid or blacklisted token detected for path: {}", path);
            return unauthorizedResponse(exchange, "Invalid or revoked token");
        }

        // Extract JTI for logging
        String jti = jwtValidationService.extractJti(token);
        logger.debug("JWT validation passed for path: {} (JTI: {})", path, jti);

        // Token is valid, continue with request
        return chain.filter(exchange);
    }

    /**
     * Check if the endpoint is public (doesn't require JWT validation)
     */
    private boolean isPublicEndpoint(String path) {
        return path.startsWith("/auth/") || 
               path.equals("/auth") ||
               path.startsWith("/actuator/");
    }

    /**
     * Return 401 Unauthorized response
     */
    private Mono<Void> unauthorizedResponse(ServerWebExchange exchange, String message) {
        ServerHttpResponse response = exchange.getResponse();
        response.setStatusCode(HttpStatus.UNAUTHORIZED);
        response.getHeaders().add("X-Auth-Error", message);
        return response.setComplete();
    }

    @Override
    public int getOrder() {
        // Execute before rate limiting (-2) and user propagation (-1)
        // This ensures blacklisted tokens are rejected early
        return -3;
    }
}

