package com.smartlogistics.notification.listener;

import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

@Service
public class OrderEventListener {
    @KafkaListener(topics = "order-events", groupId = "notification-group")
    public void handleOrderEvent(String event) {
        System.out.println("Received order event: " + event);
        // TODO: Send email/SMS notification based on event state
    }
}
