package com.smartlogistics.tracking.client;

import com.smartlogistics.tracking.dto.OrderDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(name = "order-service")
public interface OrderServiceFeignClient {

    @GetMapping("/order/{orderId}")
    ResponseEntity<OrderDTO> fetchOrder(@PathVariable("orderId") Long orderId);
}
