package com.smartlogistics.auth.service;

import com.smartlogistics.auth.entity.User;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.security.Key;
import java.util.Date;
import java.util.concurrent.TimeUnit;

@Service
public class JwtService {

    private static final Logger logger = LoggerFactory.getLogger(JwtService.class);
    private static final String JWT_BLACKLIST_PREFIX = "auth:blacklist:";
    private static final String JWT_CACHE_PREFIX = "auth:jwt:";

    @Value("${jwt.secret}")
    private String secretKey;

    private final RedisTemplate<String, Object> redisTemplate;

    public JwtService(RedisTemplate<String, Object> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    public String generateToken(User user) {
        String jti = String.valueOf(System.currentTimeMillis()); // JWT ID for blacklisting
        String token = Jwts.builder()
                .setSubject(user.getUsername())
                .claim("userId", user.getUserId())
                .claim("jti", jti)
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + 1000 * 60 * 60 * 24)) // 24 hours
                .signWith(getSigningKey(), SignatureAlgorithm.HS256)
                .compact();

        // Cache the valid token for 24 hours
        cacheValidToken(token, 24 * 60 * 60);
        logger.info("Generated JWT token for user: {}", user.getUsername());
        return token;
    }

    @Cacheable(value = "jwtUsername", key = "#token", unless = "#result == null")
    public String extractUsername(String token) {
        if (isTokenBlacklisted(token)) {
            logger.warn("Attempted to extract username from blacklisted token");
            return null;
        }
        return getClaimsFromToken(token).getSubject();
    }

    public boolean isTokenValid(String token) {
        try {
            if (isTokenBlacklisted(token)) {
                logger.warn("Token validation failed: token is blacklisted");
                return false;
            }

            // Check if token is cached as valid
            String cacheKey = JWT_CACHE_PREFIX + token.hashCode();
            if (Boolean.TRUE.equals(redisTemplate.hasKey(cacheKey))) {
                logger.debug("Token validation from cache: valid");
                return true;
            }

            // Validate token and cache result
            Claims claims = getClaimsFromToken(token);
            boolean isValid = !claims.getExpiration().before(new Date());
            
            if (isValid) {
                // Cache valid token for remaining time
                long expirationTime = claims.getExpiration().getTime() - System.currentTimeMillis();
                if (expirationTime > 0) {
                    cacheValidToken(token, expirationTime / 1000);
                }
            }
            
            logger.debug("Token validation result: {}", isValid);
            return isValid;
        } catch (Exception e) {
            logger.error("Token validation error: {}", e.getMessage());
            return false;
        }
    }

    public void blacklistToken(String token) {
        try {
            Claims claims = getClaimsFromToken(token);
            String jti = claims.get("jti", String.class);
            
            if (jti != null) {
                // Calculate remaining time until expiration
                long expirationTime = claims.getExpiration().getTime() - System.currentTimeMillis();
                if (expirationTime > 0) {
                    String blacklistKey = JWT_BLACKLIST_PREFIX + jti;
                    redisTemplate.opsForValue().set(blacklistKey, true, expirationTime, TimeUnit.MILLISECONDS);
                    
                    // Remove from valid token cache
                    String cacheKey = JWT_CACHE_PREFIX + token.hashCode();
                    redisTemplate.delete(cacheKey);
                    
                    logger.info("Token blacklisted with JTI: {}", jti);
                }
            }
        } catch (Exception e) {
            logger.error("Error blacklisting token: {}", e.getMessage());
        }
    }

    private boolean isTokenBlacklisted(String token) {
        try {
            Claims claims = getClaimsFromToken(token);
            String jti = claims.get("jti", String.class);
            
            if (jti != null) {
                String blacklistKey = JWT_BLACKLIST_PREFIX + jti;
                boolean isBlacklisted = Boolean.TRUE.equals(redisTemplate.hasKey(blacklistKey));
                if (isBlacklisted) {
                    logger.debug("Token with JTI {} is blacklisted", jti);
                }
                return isBlacklisted;
            }
            return false;
        } catch (Exception e) {
            logger.error("Error checking token blacklist: {}", e.getMessage());
            return true; // Assume blacklisted if we can't verify
        }
    }

    private void cacheValidToken(String token, long ttlSeconds) {
        try {
            String cacheKey = JWT_CACHE_PREFIX + token.hashCode();
            redisTemplate.opsForValue().set(cacheKey, true, ttlSeconds, TimeUnit.SECONDS);
            logger.debug("Cached valid token for {} seconds", ttlSeconds);
        } catch (Exception e) {
            logger.error("Error caching valid token: {}", e.getMessage());
        }
    }

    private Claims getClaimsFromToken(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(getSigningKey())
                .build()
                .parseClaimsJws(token)
                .getBody();
    }

    private Key getSigningKey() {
        byte[] keyBytes = Decoders.BASE64.decode(secretKey);
        return Keys.hmacShaKeyFor(keyBytes);
    }
}
