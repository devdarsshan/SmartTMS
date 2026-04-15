package com.smartlogistics.order.events;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.smartlogistics.order.entity.Order;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.ZoneOffset;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ExecutionException;

@Component
public class OrderEventPublisher {
    private static final Logger logger = LoggerFactory.getLogger(OrderEventPublisher.class);

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;
    private final String orderEventsTopic;
    private final int maxAttempts;
    private final long backoffMs;

    public OrderEventPublisher(KafkaTemplate<String, String> kafkaTemplate,
                               ObjectMapper objectMapper,
                               @Value("${app.kafka.topics.order-events}") String orderEventsTopic,
                               @Value("${app.kafka.publish.max-attempts}") int maxAttempts,
                               @Value("${app.kafka.publish.backoff-ms}") long backoffMs) {
        this.kafkaTemplate = kafkaTemplate;
        this.objectMapper = objectMapper;
        this.orderEventsTopic = orderEventsTopic;
        this.maxAttempts = maxAttempts;
        this.backoffMs = backoffMs;
    }

    public void publishOrderCreated(Order order, Long createdByUserId, String createdByRole) {
        Map<String, Object> payload = baseEvent("ORDER_CREATED", order);
        payload.put("createdByUserId", createdByUserId);
        payload.put("createdByRole", createdByRole);
        send(order.getOrderId(), payload);
    }

    public void publishOrderAssigned(Order order) {
        Map<String, Object> payload = baseEvent("ORDER_ASSIGNED", order);
        payload.put("assignedAt", toIso(order.getAssignedAt()));
        send(order.getOrderId(), payload);
    }

    public void publishOrderInTransit(Order order) {
        Map<String, Object> payload = baseEvent("ORDER_IN_TRANSIT", order);
        payload.put("inTransitAt", toIso(order.getInTransitAt()));
        send(order.getOrderId(), payload);
    }

    public void publishOrderDelivered(Order order) {
        Map<String, Object> payload = baseEvent("ORDER_DELIVERED", order);
        payload.put("deliveredAt", toIso(order.getDeliveredAt()));
        send(order.getOrderId(), payload);
    }

    private Map<String, Object> baseEvent(String eventType, Order order) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("schemaVersion", 1);
        payload.put("eventId", UUID.randomUUID().toString());
        payload.put("eventType", eventType);
        payload.put("occurredAt", Instant.now().toString());
        payload.put("source", "order-service");
        payload.put("orderId", order.getOrderId());
        payload.put("orderType", order.getOrderType().name());
        payload.put("status", order.getStatus().name());
        payload.put("vehicleId", order.getVehicleId());
        payload.put("from", order.getFrom());
        payload.put("to", order.getTo());
        payload.put("createdAt", toIso(order.getCreatedAt()));
        payload.put("assignedAt", toIso(order.getAssignedAt()));
        payload.put("inTransitAt", toIso(order.getInTransitAt()));
        payload.put("deliveredAt", toIso(order.getDeliveredAt()));
        return payload;
    }

    private void send(Long orderId, Map<String, Object> payload) {
        try {
            String message = objectMapper.writeValueAsString(payload);
            int attempts = 0;
            while (attempts < maxAttempts) {
                attempts++;
                try {
                    kafkaTemplate.send(orderEventsTopic, String.valueOf(orderId), message).get();
                    logger.info("Published {} event for order {}", payload.get("eventType"), orderId);
                    return;
                } catch (InterruptedException ex) {
                    Thread.currentThread().interrupt();
                    throw ex;
                } catch (ExecutionException ex) {
                    if (attempts >= maxAttempts) {
                        throw ex;
                    }
                    logger.warn("Retrying {} for order {} after Kafka publish failure on attempt {}", payload.get("eventType"), orderId, attempts);
                    Thread.sleep(backoffMs);
                }
            }
        } catch (JsonProcessingException | InterruptedException | ExecutionException ex) {
            if (ex instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
            throw new RuntimeException("Failed to publish order event for order " + orderId, ex);
        }
    }

    private String toIso(java.time.LocalDateTime value) {
        if (value == null) {
            return null;
        }
        return value.atOffset(ZoneOffset.UTC).toInstant().toString();
    }
}
