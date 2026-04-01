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
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class AuthService {

    private static final Logger logger = LoggerFactory.getLogger(AuthService.class);

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuditService auditService;

    public AuthService(UserRepository userRepository, PasswordEncoder passwordEncoder, 
                       JwtService jwtService, AuditService auditService) {
        this.passwordEncoder = passwordEncoder;
        this.userRepository = userRepository;
        this.jwtService = jwtService;
        this.auditService = auditService;
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
        User user = userRepository.findByUsername(request.getUsername())
                .orElseThrow(() -> {
                    auditService.logFailedLoginAttempt(request.getUsername(), "UNKNOWN");
                    return new UnauthorizedException("Invalid username or password");
                });

        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            auditService.logFailedLoginAttempt(request.getUsername(), "UNKNOWN");
            throw new UnauthorizedException("Invalid username or password");
        }
        
        String token = jwtService.generateToken(user);
        auditService.logLoginAttempt(request.getUsername(), true, "Login successful", "UNKNOWN");
        
        logger.info("User logged in successfully: {}", request.getUsername());
        return new AuthResponse(token, user.getUserId());
    }

    public java.util.Optional<User> getUserByUsername(String username) {
        logger.debug("Fetching user from database: {}", username);
        return userRepository.findByUsername(username);
    }
}
