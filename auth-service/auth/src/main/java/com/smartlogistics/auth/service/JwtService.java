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
import org.springframework.stereotype.Service;

import java.security.Key;
import java.util.Date;

@Service
public class JwtService {

    private static final Logger logger = LoggerFactory.getLogger(JwtService.class);
    private static final long TOKEN_EXPIRATION_MS = 24 * 60 * 60 * 1000; // 24 hours

    @Value("${jwt.secret}")
    private String secretKey;

    public String generateToken(User user) {
        long currentTimeMs = System.currentTimeMillis();
        
        String token = Jwts.builder()
                .setSubject(user.getUsername())
                .claim("userId", user.getUserId())
                .setIssuedAt(new Date(currentTimeMs))
                .setExpiration(new Date(currentTimeMs + TOKEN_EXPIRATION_MS))
                .signWith(getSigningKey(), SignatureAlgorithm.HS256)
                .compact();
        
        logger.info("Generated JWT token for user: {}", user.getUsername());
        return token;
    }

    /**
     * Extract username from token.
     * Note: Always check token validity with isTokenValid() before trusting the extracted username.
     */
    public String extractUsername(String token) {
        try {
            return getClaimsFromToken(token).getSubject();
        } catch (Exception e) {
            logger.error("Error extracting username from token: {}", e.getMessage());
            return null;
        }
    }

    public boolean isTokenValid(String token) {
        try {
            Claims claims = getClaimsFromToken(token);
            // Validate token expiration
            boolean isValid = !claims.getExpiration().before(new Date());
            
            logger.debug("Token validation result: {}", isValid);
            return isValid;
        } catch (Exception e) {
            logger.error("Token validation error: {}", e.getMessage());
            return false;
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
