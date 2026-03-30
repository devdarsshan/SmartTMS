package com.smartlogistics.auth.service;

import com.smartlogistics.auth.audit.AuditService;
import com.smartlogistics.auth.dto.AuthResponse;
import com.smartlogistics.auth.dto.LoginRequest;
import com.smartlogistics.auth.dto.RegisterRequest;
import com.smartlogistics.auth.entity.User;
import com.smartlogistics.auth.exception.UnauthorizedException;
import com.smartlogistics.auth.exception.UserAlreadyExistsException;
import com.smartlogistics.auth.repo.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.concurrent.TimeUnit;

@Service
public class AuthService {

    private static final Logger logger = LoggerFactory.getLogger(AuthService.class);
    private static final String USER_CACHE_PREFIX = "auth:user:";
    private static final String RATE_LIMIT_PREFIX = "auth:rate_limit:";

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuditService auditService;
    private final RedisTemplate<String, Object> redisTemplate;

    public AuthService(UserRepository userRepository, PasswordEncoder passwordEncoder, 
                       JwtService jwtService, AuditService auditService, 
                       RedisTemplate<String, Object> redisTemplate) {
        this.passwordEncoder = passwordEncoder;
        this.userRepository = userRepository;
        this.jwtService = jwtService;
        this.auditService = auditService;
        this.redisTemplate = redisTemplate;
    }

    public void register(RegisterRequest request) {
        // Check if user already exists
        if (userRepository.findByUsername(request.getUsername()).isPresent()) {
            auditService.logRegistration(request.getUsername(),false, "User already exists");
            throw new UserAlreadyExistsException("Username already exists: " + request.getUsername());
        }

        // Create new user
        User user = new User();
        user.setUsername(request.getUsername());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        
        User savedUser = userRepository.save(user);
        
        // Cache the user for quick access
        cacheUser(savedUser);
        
        auditService.logRegistration(request.getUsername(),true, "User registered successfully");
        logger.info("User registered successfully: {}", request.getUsername());
    }

    public AuthResponse login(LoginRequest request) {
        String clientIp = getClientIp();
        
        // Check rate limiting for this IP
        if (isRateLimited(clientIp)) {
            auditService.logFailedLoginAttempt(request.getUsername(), clientIp);
            throw new UnauthorizedException("Rate limit exceeded. Please try again later.");
        }
        
        // Try to get user from cache first
        User user = getCachedUser(request.getUsername());
        if (user == null) {
            user = userRepository.findByUsername(request.getUsername())
                    .orElseThrow(() -> {
                        recordFailedLoginAttempt(clientIp);
                        auditService.logFailedLoginAttempt(request.getUsername(), clientIp);
                        return new UnauthorizedException("Invalid username or password");
                    });
            // Cache the user for future lookups
            cacheUser(user);
        }

        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            recordFailedLoginAttempt(clientIp);
            auditService.logFailedLoginAttempt(request.getUsername(), clientIp);
            throw new UnauthorizedException("Invalid username or password");
        }

        // Reset rate limiting on successful login
        resetRateLimiting(clientIp);
        
        String token = jwtService.generateToken(user);
        auditService.logLoginAttempt(request.getUsername(), true, "Login successful", clientIp);
        
        logger.info("User logged in successfully: {}", request.getUsername());
        return new AuthResponse(token, user.getUserId());
    }

    @Cacheable(value = "userByUsername", key = "#username", unless = "#result == null")
    public User getCachedUser(String username) {
        logger.debug("Attempting to retrieve user from cache: {}", username);
        return null; // Spring will handle caching, this is just for cache miss logging
    }

    @CacheEvict(value = "userByUsername", key = "#user.username")
    public void evictUserCache(User user) {
        logger.debug("Evicted user from cache: {}", user.getUsername());
    }

    private void cacheUser(User user) {
        try {
            String cacheKey = USER_CACHE_PREFIX + user.getUsername();
            redisTemplate.opsForValue().set(cacheKey, user, 1, TimeUnit.HOURS);
            logger.debug("Cached user: {}", user.getUsername());
        } catch (Exception e) {
            logger.error("Error caching user: {}", e.getMessage());
        }
    }

    private boolean isRateLimited(String clientIp) {
        try {
            String rateLimitKey = RATE_LIMIT_PREFIX + clientIp;
            Integer attempts = (Integer) redisTemplate.opsForValue().get(rateLimitKey);
            boolean limited = attempts != null && attempts >= 5; // Max 5 attempts per minute
            
            if (limited) {
                logger.warn("Rate limit exceeded for IP: {}", clientIp);
            }
            
            return limited;
        } catch (Exception e) {
            logger.error("Error checking rate limit: {}", e.getMessage());
            return false; // Don't block on cache errors
        }
    }

    private void recordFailedLoginAttempt(String clientIp) {
        try {
            String rateLimitKey = RATE_LIMIT_PREFIX + clientIp;
            Integer attempts = (Integer) redisTemplate.opsForValue().get(rateLimitKey);
            attempts = (attempts == null) ? 1 : attempts + 1;
            
            redisTemplate.opsForValue().set(rateLimitKey, attempts, 1, TimeUnit.MINUTES);
            logger.debug("Recorded failed login attempt for IP: {} (attempt: {})", clientIp, attempts);
        } catch (Exception e) {
            logger.error("Error recording failed login attempt: {}", e.getMessage());
        }
    }

    private void resetRateLimiting(String clientIp) {
        try {
            String rateLimitKey = RATE_LIMIT_PREFIX + clientIp;
            redisTemplate.delete(rateLimitKey);
            logger.debug("Reset rate limiting for IP: {}", clientIp);
        } catch (Exception e) {
            logger.error("Error resetting rate limit: {}", e.getMessage());
        }
    }

    private String getClientIp() {
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attributes != null) {
            String xForwardedFor = attributes.getRequest().getHeader("X-Forwarded-For");
            if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
                return xForwardedFor.split(",")[0].trim();
            }
            return attributes.getRequest().getRemoteAddr();
        }
        return "UNKNOWN";
    }
}
