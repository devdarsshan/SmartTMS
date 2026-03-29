package com.smartlogistics.order.client;

import com.smartlogistics.order.dto.UserProfileDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(name = "user-service")
public interface UserServiceFeignClient {

    @GetMapping("/user/fetchProfile/{authUserId}")
    ResponseEntity<UserProfileDTO> getUserProfile(@PathVariable("authUserId") Long authUserId);
}
