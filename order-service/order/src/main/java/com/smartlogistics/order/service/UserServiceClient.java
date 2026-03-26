package com.smartlogistics.order.service;

import com.smartlogistics.order.client.UserServiceFeignClient;
import com.smartlogistics.order.dto.UserProfileDTO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

@Service
public class UserServiceClient {
    private static final Logger logger = LoggerFactory.getLogger(UserServiceClient.class);

    private final UserServiceFeignClient userServiceFeignClient;

    public UserServiceClient(UserServiceFeignClient userServiceFeignClient) {
        this.userServiceFeignClient = userServiceFeignClient;
    }

    public UserProfileDTO getUserProfile(Long authUserId) {
        logger.info("Fetching user profile for authUserId: {}", authUserId);
        try {
            ResponseEntity<UserProfileDTO> response = userServiceFeignClient.getUserProfile(authUserId);
            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                return response.getBody();
            }
            return null;
        } catch (Exception e) {
            logger.error("Error fetching user profile from user-service for authUserId: {}", authUserId, e);
            throw new RuntimeException("Failed to communicate with user-service", e);
        }
    }
}
