package com.smartlogistics.vehicleservice.events;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.smartlogistics.vehicleservice.entity.Vehicle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ExecutionException;

@Component
public class VehicleEventPublisher {
    private static final Logger logger = LoggerFactory.getLogger(VehicleEventPublisher.class);

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;
    private final String vehicleEventsTopic;
    private final int maxAttempts;
    private final long backoffMs;

    public VehicleEventPublisher(KafkaTemplate<String, String> kafkaTemplate,
                                 ObjectMapper objectMapper,
                                 @Value("${app.kafka.topics.vehicle-events}") String vehicleEventsTopic,
                                 @Value("${app.kafka.publish.max-attempts}") int maxAttempts,
                                 @Value("${app.kafka.publish.backoff-ms}") long backoffMs) {
        this.kafkaTemplate = kafkaTemplate;
        this.objectMapper = objectMapper;
        this.vehicleEventsTopic = vehicleEventsTopic;
        this.maxAttempts = maxAttempts;
        this.backoffMs = backoffMs;
    }

    public void publishDriverAssigned(Vehicle vehicle) {
        Map<String, Object> payload = baseEvent("DRIVER_ASSIGNED_TO_VEHICLE", vehicle);
        send(vehicle.getVehicleId(), payload);
    }

    public void publishDriverUnassigned(Vehicle vehicle, Long previousDriverId) {
        Map<String, Object> payload = baseEvent("DRIVER_UNASSIGNED_FROM_VEHICLE", vehicle);
        payload.put("driverId", previousDriverId);
        send(vehicle.getVehicleId(), payload);
    }

    public void publishVehicleStatusUpdated(Vehicle vehicle) {
        Map<String, Object> payload = baseEvent("VEHICLE_STATUS_UPDATED", vehicle);
        send(vehicle.getVehicleId(), payload);
    }

    private Map<String, Object> baseEvent(String eventType, Vehicle vehicle) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("schemaVersion", 1);
        payload.put("eventId", UUID.randomUUID().toString());
        payload.put("eventType", eventType);
        payload.put("occurredAt", Instant.now().toString());
        payload.put("source", "vehicle-service");
        payload.put("vehicleId", vehicle.getVehicleId());
        payload.put("driverId", vehicle.getDriverId());
        payload.put("vehicleStatus", vehicle.getVehicleStatus() == null ? null : vehicle.getVehicleStatus().name());
        payload.put("orderIds", vehicle.getOrderIds());
        payload.put("from", vehicle.getFrom());
        payload.put("to", vehicle.getTo());
        payload.put("through", vehicle.getThrough());
        return payload;
    }

    private void send(Long vehicleId, Map<String, Object> payload) {
        try {
            String message = objectMapper.writeValueAsString(payload);
            int attempts = 0;
            while (attempts < maxAttempts) {
                attempts++;
                try {
                    kafkaTemplate.send(vehicleEventsTopic, String.valueOf(vehicleId), message).get();
                    logger.info("Published {} event for vehicle {}", payload.get("eventType"), vehicleId);
                    return;
                } catch (InterruptedException ex) {
                    Thread.currentThread().interrupt();
                    throw ex;
                } catch (ExecutionException ex) {
                    if (attempts >= maxAttempts) {
                        throw ex;
                    }
                    logger.warn("Retrying {} for vehicle {} after Kafka publish failure on attempt {}", payload.get("eventType"), vehicleId, attempts);
                    Thread.sleep(backoffMs);
                }
            }
        } catch (JsonProcessingException | InterruptedException | ExecutionException ex) {
            if (ex instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
            throw new RuntimeException("Failed to publish vehicle event for vehicle " + vehicleId, ex);
        }
    }
}
