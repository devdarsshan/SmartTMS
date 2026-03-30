package com.smartlogistics.order.controller;

import com.smartlogistics.order.dto.CreateOrderRequest;
import com.smartlogistics.order.dto.UpdateOrderStatusRequest;
import com.smartlogistics.order.entity.Order;
import com.smartlogistics.order.service.OrderService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/order")
public class OrderController {
    private static final Logger logger = LoggerFactory.getLogger(OrderController.class);

    private final OrderService orderService;

    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    @PostMapping("/create")
    public ResponseEntity<Order> createOrder(@Valid @RequestBody CreateOrderRequest request,
                                             @RequestHeader("X-User-Id") String userIdHeader) {
        logger.info("Creating order for user {}", userIdHeader);
        return ResponseEntity.ok(orderService.createOrder(request, userIdHeader));
    }

    @PostMapping("/{orderId}/assign")
    public ResponseEntity<Order> assignOrder(@PathVariable("orderId") Long orderId) {
        logger.info("Assigning vehicle to order {}", orderId);
        return ResponseEntity.ok(orderService.assignOrder(orderId));
    }

    @PutMapping("/{orderId}/status")
    public ResponseEntity<Order> updateOrderStatus(@PathVariable("orderId") Long orderId,
                                                   @Valid @RequestBody UpdateOrderStatusRequest request) {
        logger.info("Updating order {} status to {}", orderId, request.getStatus());
        return ResponseEntity.ok(orderService.updateOrderStatus(orderId, request));
    }

    @GetMapping("/{orderId}")
    public ResponseEntity<Order> fetchOrder(@PathVariable("orderId") Long orderId) {
        logger.info("Fetching order {}", orderId);
        return orderService.fetchOrder(orderId);
    }

    @GetMapping("/all")
    public ResponseEntity<List<Order>> fetchAllOrders() {
        logger.info("Fetching all orders");
        return orderService.fetchAllOrders();
    }
}
