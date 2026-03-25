package com.smartlogistics.vehicleservice.client;

import com.smartlogistics.vehicleservice.dto.DriverInfoDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(name = "user-service", url = "http://localhost:8081")
public interface UserServiceFeignClient {

    @GetMapping("/user/fetchProfile/{authUserId}")
    ResponseEntity<DriverInfoDTO> getDriverInfo(@PathVariable("authUserId") Long authUserId);
}

