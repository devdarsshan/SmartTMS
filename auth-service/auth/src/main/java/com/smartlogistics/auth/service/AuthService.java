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
    private static final String RATE_LIMIT_PREFIX = "auth:rate_limit:";
    private static final int MAX_LOGIN_ATTEMPTS = 5;
    private static final int RATE_LIMIT_DURATION_MINUTES = 1;

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
        if (userRepository.findByUsername(request.getUsername()).isPresent()) {
            auditService.logRegistration(request.getUsername(), false, "User already exists");
            throw new UserAlreadyExistsException("Username already exists: " + request.getUsername());
        }

        User user = new User();
        user.setUsername(request.getUsername());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        
        userRepository.save(user);
        
        auditService.logRegistration(request.getUsername(), true, "User registered successfully");
        logger.info("User registered successfully: {}", request.getUsername());
    }

    public AuthResponse login(LoginRequest request) {
        String clientIp = getClientIp();
        
        // Check rate limiting for this IP
        if (isRateLimited(clientIp)) {
            auditService.logFailedLoginAttempt(request.getUsername(), clientIp);
            throw new UnauthorizedException("Rate limit exceeded. Please try again later.");
        }
        
        User user = getUserByUsername(request.getUsername())
                .orElseThrow(() -> {
                    recordFailedLoginAttempt(clientIp);
                    auditService.logFailedLoginAttempt(request.getUsername(), clientIp);
                    return new UnauthorizedException("Invalid username or password");
                });

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


    @Cacheable(value = "userByUsername", key = "#username", unless = "#result == null || !#result.isPresent()")
    public java.util.Optional<User> getUserByUsername(String username) {
        logger.debug("Fetching user from database: {}", username);
        return userRepository.findByUsername(username);
    }

    @CacheEvict(value = "userByUsername", key = "#username")
    public void evictUserCache(String username) {
        logger.debug("Evicted user from cache: {}", username);
    }

    private boolean isRateLimited(String clientIp) {
        try {
            String rateLimitKey = RATE_LIMIT_PREFIX + clientIp;
            Integer attempts = (Integer) redisTemplate.opsForValue().get(rateLimitKey);
            boolean limited = attempts != null && attempts >= MAX_LOGIN_ATTEMPTS;
            
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
            
            redisTemplate.opsForValue().set(rateLimitKey, attempts, RATE_LIMIT_DURATION_MINUTES, TimeUnit.MINUTES);
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
