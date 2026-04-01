package com.smartlogistics.apigateway.service;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.util.Date;

/**
 * JWT Validation Service for API Gateway
 * Checks JWT blacklist and valid token cache maintained by Auth Service
 * This prevents revoked/blacklisted tokens from accessing protected endpoints
 */
@Service
public class JwtValidationService {

    private static final Logger logger = LoggerFactory.getLogger(JwtValidationService.class);
    private static final String JWT_BLACKLIST_PREFIX = "auth:jwt:blacklist:";
    private static final String JWT_VALIDATION_CACHE_PREFIX = "auth:jwt:valid:";

    @Value("${jwt.secret}")
    private String secretKey;

    private final RedisTemplate<String, Object> redisTemplate;

    public JwtValidationService(RedisTemplate<String, Object> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    /**
     * Validates JWT token by checking:
     * 1. Token is not blacklisted
     * 2. Token is not expired
     * 3. Token signature is valid
     * 
     * @param token JWT token string
     * @return true if token is valid and not blacklisted
     */
    public boolean isTokenValid(String token) {
        try {
            // Parse and validate token
            Claims claims = getClaimsFromToken(token);
            
            // Extract JTI (JWT ID) for blacklist check
            String jti = claims.get("jti", String.class);
            
            if (jti == null) {
                logger.warn("Token validation failed: missing JTI claim");
                return false;
            }

            // Check if token is blacklisted
            if (isTokenBlacklisted(jti)) {
                logger.warn("Token validation failed: token with JTI {} is blacklisted", jti);
                return false;
            }

            // Check if token is in valid cache (fast path)
            if (isTokenInValidCache(jti)) {
                logger.debug("Token validation from cache: valid (JTI: {})", jti);
                return true;
            }

            // Validate token expiration
            boolean isValid = !claims.getExpiration().before(new Date());
            
            if (isValid) {
                logger.debug("Token validation successful for JTI: {}", jti);
            } else {
                logger.debug("Token validation failed: token expired (JTI: {})", jti);
            }
            
            return isValid;
        } catch (Exception e) {
            logger.error("Token validation error: {}", e.getMessage());
            return false;
        }
    }

    /**
     * Extract username from JWT token
     * Note: Should only be called after token validation
     */
    public String extractUsername(String token) {
        try {
            return getClaimsFromToken(token).getSubject();
        } catch (Exception e) {
            logger.error("Error extracting username from token: {}", e.getMessage());
            return null;
        }
    }

    /**
     * Extract JTI (JWT ID) from token
     */
    public String extractJti(String token) {
        try {
            Claims claims = getClaimsFromToken(token);
            return claims.get("jti", String.class);
        } catch (Exception e) {
            logger.error("Error extracting JTI from token: {}", e.getMessage());
            return null;
        }
    }

    /**
     * Check if token is blacklisted by checking Redis cache
     * Key format: auth:jwt:blacklist:{jti}
     */
    private boolean isTokenBlacklisted(String jti) {
        try {
            String blacklistKey = JWT_BLACKLIST_PREFIX + jti;
            boolean isBlacklisted = Boolean.TRUE.equals(redisTemplate.hasKey(blacklistKey));
            
            if (isBlacklisted) {
                logger.debug("Token with JTI {} found in blacklist", jti);
            }
            
            return isBlacklisted;
        } catch (Exception e) {
            logger.error("Error checking token blacklist for JTI {}: {}", jti, e.getMessage());
            // Fail secure: assume blacklisted if Redis is unavailable
            return true;
        }
    }

    /**
     * Check if token is in valid token cache (optimization)
     * Key format: auth:jwt:valid:{jti}
     */
    private boolean isTokenInValidCache(String jti) {
        try {
            String validCacheKey = JWT_VALIDATION_CACHE_PREFIX + jti;
            return Boolean.TRUE.equals(redisTemplate.hasKey(validCacheKey));
        } catch (Exception e) {
            logger.debug("Error checking valid token cache for JTI {}: {}", jti, e.getMessage());
            return false; // Don't fail if cache check fails, continue with normal validation
        }
    }

    /**
     * Parse JWT token and extract claims
     */
    private Claims getClaimsFromToken(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(getSigningKey())
                .build()
                .parseClaimsJws(token)
                .getBody();
    }

    /**
     * Get signing key for JWT validation
     */
    private SecretKey getSigningKey() {
        byte[] keyBytes = Decoders.BASE64.decode(secretKey);
        return Keys.hmacShaKeyFor(keyBytes);
    }
}

