package com.smartlogistics.ai.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.Map;

@FeignClient(name = "tracking-service")
public interface TrackingServiceFeignClient {

    @GetMapping("/track/vehicle/{vehicleId}")
    ResponseEntity<Map<String, Object>> getLiveLocation(@PathVariable("vehicleId") Long vehicleId);
}
