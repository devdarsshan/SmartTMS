package com.smartlogistics.tracking.service;

import com.smartlogistics.tracking.client.UserServiceFeignClient;
import com.smartlogistics.tracking.dto.UserProfileDTO;
import com.smartlogistics.tracking.exceptions.ResourceNotFoundException;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

@Service
public class UserServiceClient {
    private final UserServiceFeignClient userServiceFeignClient;

    public UserServiceClient(UserServiceFeignClient userServiceFeignClient) {
        this.userServiceFeignClient = userServiceFeignClient;
    }

    public UserProfileDTO fetchUserProfile(Long authUserId) {
        ResponseEntity<UserProfileDTO> response = userServiceFeignClient.fetchUserProfile(authUserId);
        if (response.getBody() == null) {
            throw new ResourceNotFoundException("User profile not found for auth user " + authUserId);
        }
        return response.getBody();
    }
}
