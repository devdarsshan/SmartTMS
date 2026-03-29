package com.smartlogistics.auth.audit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Service
public class AuditService {
    private static final Logger logger = LoggerFactory.getLogger(AuditService.class);
    private static final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public void logRegistration(String username, boolean success, String reason) {
        String message = String.format(
                "[AUDIT] Registration | Username: %s | Success: %s | Reason: %s | Timestamp: %s",
                username, success, reason, LocalDateTime.now().format(formatter)
        );
        if (success) {
            logger.info(message);
        } else {
            logger.warn(message);
        }
    }

    public void logLoginAttempt(String username, boolean success, String reason, String clientIp) {
        String message = String.format(
                "[AUDIT] Login Attempt | Username: %s | Success: %s | Reason: %s | ClientIP: %s | Timestamp: %s",
                username, success, reason, clientIp, LocalDateTime.now().format(formatter)
        );
        if (success) {
            logger.info(message);
        } else {
            logger.warn(message);
        }
    }

    public void logFailedLoginAttempt(String username, String clientIp) {
        String message = String.format(
                "[AUDIT] Failed Login | Username: %s | ClientIP: %s | Timestamp: %s",
                username, clientIp, LocalDateTime.now().format(formatter)
        );
        logger.warn(message);
    }

    public void logSecurityEvent(String event, String details) {
        String message = String.format(
                "[SECURITY] %s | Details: %s | Timestamp: %s",
                event, details, LocalDateTime.now().format(formatter)
        );
        logger.warn(message);
    }

    public void logAuthenticationEvent(String username, String action, boolean success) {
        String message = String.format(
                "[AUDIT] Authentication | Username: %s | Action: %s | Success: %s | Timestamp: %s",
                username, action, success, LocalDateTime.now().format(formatter)
        );
        logger.info(message);
    }
}

