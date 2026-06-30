package com.smartlogistics.ai.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.Map;

@FeignClient(name = "order-service")
public interface OrderServiceFeignClient {

    @GetMapping("/order/{orderId}")
    ResponseEntity<Map<String, Object>> getOrderDetails(@PathVariable("orderId") Long orderId);
}
