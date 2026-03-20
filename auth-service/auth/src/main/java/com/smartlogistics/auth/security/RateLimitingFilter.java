package com.smartlogistics.auth.security;

import com.smartlogistics.auth.exception.RateLimitExceededException;
import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Component;
import java.io.IOException;

@Component
public class RateLimitingFilter implements Filter {

    private final RateLimitingBucket rateLimitingBucket = new RateLimitingBucket();

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        
        HttpServletRequest httpRequest = (HttpServletRequest) request;
        String clientIp = getClientIp(httpRequest);
        
        // Only apply rate limiting to auth endpoints
        String requestUri = httpRequest.getRequestURI();
        if (requestUri.contains("/auth/")) {
            if (rateLimitingBucket.resolveBucket(clientIp).tryConsume(1)) {
                chain.doFilter(request, response);
            } else {
                throw new RateLimitExceededException("Too many requests. Please try again later.");
            }
        } else {
            chain.doFilter(request, response);
        }
    }

    private String getClientIp(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            return xForwardedFor.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}

