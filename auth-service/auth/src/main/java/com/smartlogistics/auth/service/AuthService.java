package com.smartlogistics.auth.service;

import com.smartlogistics.auth.audit.AuditService;
import com.smartlogistics.auth.dto.AuthResponse;
import com.smartlogistics.auth.dto.LoginRequest;
import com.smartlogistics.auth.dto.RegisterRequest;
import com.smartlogistics.auth.entity.User;
import com.smartlogistics.auth.exception.UnauthorizedException;
import com.smartlogistics.auth.exception.UserAlreadyExistsException;
import com.smartlogistics.auth.repo.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

@Service
public class AuthService {

    private UserRepository userRepository;
    private PasswordEncoder passwordEncoder;
    private JwtService jwtService;
    private AuditService auditService;

    public AuthService(UserRepository userRepository, PasswordEncoder passwordEncoder, 
                       JwtService jwtService, AuditService auditService) {
        this.passwordEncoder = passwordEncoder;
        this.userRepository = userRepository;
        this.jwtService = jwtService;
        this.auditService = auditService;
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
        
        userRepository.save(user);
        auditService.logRegistration(request.getUsername(),true, "User registered successfully");
    }

    public AuthResponse login(LoginRequest request) {
        String clientIp = getClientIp();
        
        User user = userRepository.findByUsername(request.getUsername())
                .orElseThrow(() -> {
                    auditService.logFailedLoginAttempt(request.getUsername(), clientIp);
                    return new UnauthorizedException("Invalid username or password");
                });

        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            auditService.logFailedLoginAttempt(request.getUsername(), clientIp);
            throw new UnauthorizedException("Invalid username or password");
        }

        String token = jwtService.generateToken(user);
        auditService.logLoginAttempt(request.getUsername(), true, "Login successful", clientIp);
        
        return new AuthResponse(token,user.getUserId());
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
