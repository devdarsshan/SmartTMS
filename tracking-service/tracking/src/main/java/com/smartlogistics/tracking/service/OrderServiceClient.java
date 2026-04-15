package com.smartlogistics.tracking.service;

import com.smartlogistics.tracking.client.OrderServiceFeignClient;
import com.smartlogistics.tracking.dto.OrderDTO;
import com.smartlogistics.tracking.exceptions.ResourceNotFoundException;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

@Service
public class OrderServiceClient {
    private final OrderServiceFeignClient orderServiceFeignClient;

    public OrderServiceClient(OrderServiceFeignClient orderServiceFeignClient) {
        this.orderServiceFeignClient = orderServiceFeignClient;
    }

    public OrderDTO fetchOrder(Long orderId) {
        ResponseEntity<OrderDTO> response = orderServiceFeignClient.fetchOrder(orderId);
        if (response.getBody() == null) {
            throw new ResourceNotFoundException("Order with id " + orderId + " not found");
        }
        return response.getBody();
    }
}
