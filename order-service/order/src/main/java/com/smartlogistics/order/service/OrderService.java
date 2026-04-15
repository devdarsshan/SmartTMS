package com.smartlogistics.order.service;

import com.smartlogistics.order.dto.CreateOrderRequest;
import com.smartlogistics.order.dto.UpdateOrderStatusRequest;
import com.smartlogistics.order.dto.UserProfileDTO;
import com.smartlogistics.order.dto.VehicleDTO;
import com.smartlogistics.order.entity.Order;
import com.smartlogistics.order.enums.OrderStatus;
import com.smartlogistics.order.enums.VehicleStatus;
import com.smartlogistics.order.events.OrderEventPublisher;
import com.smartlogistics.order.exceptions.InvalidOrderStateException;
import com.smartlogistics.order.exceptions.NoVehicleAvailableException;
import com.smartlogistics.order.exceptions.OrderNotFoundException;
import com.smartlogistics.order.exceptions.UnauthorizedOperationException;
import com.smartlogistics.order.repo.OrderRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class OrderService {
    private static final Logger logger = LoggerFactory.getLogger(OrderService.class);

    private final OrderRepository orderRepository;
    private final UserServiceClient userServiceClient;
    private final VehicleServiceClient vehicleServiceClient;
    private final OrderEventPublisher orderEventPublisher;

    public OrderService(OrderRepository orderRepository,
                        UserServiceClient userServiceClient,
                        VehicleServiceClient vehicleServiceClient,
                        OrderEventPublisher orderEventPublisher) {
        this.orderRepository = orderRepository;
        this.userServiceClient = userServiceClient;
        this.vehicleServiceClient = vehicleServiceClient;
        this.orderEventPublisher = orderEventPublisher;
    }

    @Transactional
    public Order createOrder(CreateOrderRequest request, String userIdHeader) {
        logger.info("Creating order from {} to {}", request.getFrom(), request.getTo());
        Long authUserId = parseAuthUserId(userIdHeader);
        UserProfileDTO userProfile = userServiceClient.getUserProfile(authUserId);
        if (userProfile == null || userProfile.getUserRole() == null ||
                !userProfile.getUserRole().equalsIgnoreCase("DISPATCHER")) {
            throw new UnauthorizedOperationException("Only users with DISPATCHER role can create orders");
        }

        Order order = new Order();
        order.setFrom(request.getFrom());
        order.setTo(request.getTo());
        order.setCreatedAt(LocalDateTime.now());
        order.setStatus(OrderStatus.CREATED);
        order.setOrderType(request.getOrderType());

        Order savedOrder = orderRepository.save(order);
        orderEventPublisher.publishOrderCreated(savedOrder, authUserId, userProfile.getUserRole());
        return savedOrder;
    }

    @Transactional
    public Order assignOrder(Long orderId) {
        logger.info("Assigning vehicle for order {}", orderId);
        Order order = getOrderEntity(orderId);
        if (order.getStatus() != OrderStatus.CREATED) {
            throw new InvalidOrderStateException("Only CREATED orders can be assigned to a vehicle");
        }

        List<VehicleDTO> candidateVehicles = vehicleServiceClient.findVehiclesForOrder(order.getFrom(), order.getTo());
        if (candidateVehicles.isEmpty()) {
            throw new NoVehicleAvailableException("No matching vehicle is available for order " + orderId);
        }

        VehicleDTO selectedVehicle = candidateVehicles.get(0);
        vehicleServiceClient.assignOrderToVehicle(selectedVehicle.getVehicleId(), orderId);

        order.setVehicleId(selectedVehicle.getVehicleId());
        order.setStatus(OrderStatus.ASSIGNED);
        order.setAssignedAt(LocalDateTime.now());

        Order savedOrder = orderRepository.save(order);
        orderEventPublisher.publishOrderAssigned(savedOrder);
        return savedOrder;
    }

    @Transactional
    public Order updateOrderStatus(Long orderId, UpdateOrderStatusRequest request) {
        logger.info("Updating status for order {} to {}", orderId, request.getStatus());
        Order order = getOrderEntity(orderId);

        if (request.getStatus() == OrderStatus.CREATED || request.getStatus() == OrderStatus.ASSIGNED) {
            throw new InvalidOrderStateException("Order status can only be updated to IN_TRANSIT or DELIVERED through this API");
        }

        if (order.getStatus() == OrderStatus.DELIVERED) {
            throw new InvalidOrderStateException("Delivered orders cannot be updated");
        }

        if (order.getVehicleId() == null) {
            throw new InvalidOrderStateException("Order must be assigned to a vehicle before updating status");
        }

        if (request.getStatus() == OrderStatus.IN_TRANSIT) {
            if (order.getStatus() != OrderStatus.ASSIGNED) {
                throw new InvalidOrderStateException("Only ASSIGNED orders can move to IN_TRANSIT");
            }
            vehicleServiceClient.updateVehicleStatus(order.getVehicleId(), VehicleStatus.IN_TRANSIT);
            order.setStatus(OrderStatus.IN_TRANSIT);
            order.setInTransitAt(LocalDateTime.now());
            Order savedOrder = orderRepository.save(order);
            orderEventPublisher.publishOrderInTransit(savedOrder);
            return savedOrder;
        }

        if (order.getStatus() != OrderStatus.ASSIGNED && order.getStatus() != OrderStatus.IN_TRANSIT) {
            throw new InvalidOrderStateException("Only ASSIGNED or IN_TRANSIT orders can be delivered");
        }

        vehicleServiceClient.removeOrderFromVehicle(order.getVehicleId(), orderId);
        order.setStatus(OrderStatus.DELIVERED);
        order.setDeliveredAt(LocalDateTime.now());
        Order savedOrder = orderRepository.save(order);
        orderEventPublisher.publishOrderDelivered(savedOrder);
        return savedOrder;
    }

    public ResponseEntity<Order> fetchOrder(Long orderId) {
        return ResponseEntity.ok(getOrderEntity(orderId));
    }

    public ResponseEntity<List<Order>> fetchAllOrders() {
        return ResponseEntity.ok(orderRepository.findAll());
    }

    private Order getOrderEntity(Long orderId) {
        return orderRepository.findById(orderId)
                .orElseThrow(() -> new OrderNotFoundException("Order with id " + orderId + " not found"));
    }

    private Long parseAuthUserId(String userIdHeader) {
        if (userIdHeader == null || userIdHeader.trim().isEmpty()) {
            throw new UnauthorizedOperationException("X-User-Id header is required");
        }
        try {
            return Long.parseLong(userIdHeader);
        } catch (NumberFormatException e) {
            throw new UnauthorizedOperationException("X-User-Id header must be a valid number");
        }
    }
}
