package com.smartlogistics.analytics.service;

import com.smartlogistics.analytics.client.UserServiceFeignClient;
import com.smartlogistics.analytics.dto.UserProfileDTO;
import com.smartlogistics.analytics.exceptions.UnauthorizedOperationException;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

@Service
public class AnalyticsAccessService {
    private final UserServiceFeignClient userServiceFeignClient;

    public AnalyticsAccessService(UserServiceFeignClient userServiceFeignClient) {
        this.userServiceFeignClient = userServiceFeignClient;
    }

    public void requireOperationalAnalyticsAccess(String userIdHeader) {
        Long authUserId = parseAuthUserId(userIdHeader);
        ResponseEntity<UserProfileDTO> response = userServiceFeignClient.fetchUserProfile(authUserId);
        UserProfileDTO userProfile = response.getBody();
        if (userProfile == null || userProfile.getUserRole() == null) {
            throw new UnauthorizedOperationException("Unable to resolve caller profile");
        }

        String role = userProfile.getUserRole();
        if (!role.equalsIgnoreCase("ADMIN") && !role.equalsIgnoreCase("DISPATCHER")) {
            throw new UnauthorizedOperationException("Only ADMIN and DISPATCHER users can access analytics");
        }
    }

    private Long parseAuthUserId(String userIdHeader) {
        if (userIdHeader == null || userIdHeader.trim().isEmpty()) {
            throw new UnauthorizedOperationException("X-User-Id header is required");
        }
        try {
            return Long.parseLong(userIdHeader);
        } catch (NumberFormatException ex) {
            throw new UnauthorizedOperationException("X-User-Id header must be a valid number");
        }
    }
}
