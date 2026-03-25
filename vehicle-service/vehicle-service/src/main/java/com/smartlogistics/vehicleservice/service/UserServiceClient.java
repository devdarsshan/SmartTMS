package com.smartlogistics.vehicleservice.service;

import com.smartlogistics.vehicleservice.client.UserServiceFeignClient;
import com.smartlogistics.vehicleservice.dto.DriverInfoDTO;
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

    public DriverInfoDTO getDriverInfo(Long driverId) {
        logger.info("Fetching driver info for driverId: {}", driverId);
        try {
            ResponseEntity<DriverInfoDTO> response = userServiceFeignClient.getDriverInfo(driverId);
            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                logger.info("Driver info fetched successfully for driverId: {}", driverId);
                return response.getBody();
            }
            logger.warn("No driver found for driverId: {}", driverId);
            return null;
        } catch (Exception e) {
            logger.error("Error fetching driver info from user-service for driverId: {}", driverId, e);
            throw new RuntimeException("Failed to communicate with user-service", e);
        }
    }
}


