package com.smartlogistics.tracking.client;

import com.smartlogistics.tracking.dto.UserProfileDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(name = "user-service")
public interface UserServiceFeignClient {

    @GetMapping("/user/fetchProfile/{authUserId}")
    ResponseEntity<UserProfileDTO> fetchUserProfile(@PathVariable("authUserId") Long authUserId);
}
