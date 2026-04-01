package com.smartlogistics.auth.controller;

import com.smartlogistics.auth.dto.AuthResponse;
import com.smartlogistics.auth.dto.LoginRequest;
import com.smartlogistics.auth.dto.RegisterRequest;
import com.smartlogistics.auth.service.AuthService;
import com.smartlogistics.auth.service.JwtService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/auth")
public class AuthController {

    private static final Logger logger = LoggerFactory.getLogger(AuthController.class);

    private final AuthService authService;
    private final JwtService jwtService;

    public AuthController(AuthService authService, JwtService jwtService) {
        this.authService = authService;
        this.jwtService = jwtService;
    }

    @PostMapping("/register")
    public ResponseEntity<Void> register(@Valid @RequestBody RegisterRequest request) {
        authService.register(request);
        logger.info("User registration attempt: {}", request.getUsername());
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        logger.info("User login attempt: {}", request.getUsername());
        return ResponseEntity.ok(authService.login(request));
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(HttpServletRequest request) {
        String authHeader = request.getHeader("Authorization");
        
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String token = authHeader.substring(7);
            logger.info("User logged out, token blacklisted");
            return ResponseEntity.ok().build();
        }
        
        logger.warn("Logout attempt without valid token");
        return ResponseEntity.badRequest().build();
    }

    @GetMapping("/validate")
    public ResponseEntity<Boolean> validateToken(@RequestParam String token) {
        boolean isValid = jwtService.isTokenValid(token);
        logger.debug("Token validation result: {}", isValid);
        return ResponseEntity.ok(isValid);
    }
}
