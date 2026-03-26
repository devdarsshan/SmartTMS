package com.smartlogistics.order.repo;

import com.smartlogistics.order.entity.Order;
import com.smartlogistics.order.enums.OrderStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface OrderRepository extends JpaRepository<Order, Long> {

    List<Order> findByStatus(OrderStatus status);
}
