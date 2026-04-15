package com.smartlogistics.tracking.events;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.smartlogistics.tracking.dto.LocationUpdateRequest;
import com.smartlogistics.tracking.service.VehicleAssignmentSnapshot;
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
public class VehicleLocationEventPublisher {
    private static final Logger logger = LoggerFactory.getLogger(VehicleLocationEventPublisher.class);

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;
    private final String vehicleLocationEventsTopic;
    private final int maxAttempts;
    private final long backoffMs;

    public VehicleLocationEventPublisher(KafkaTemplate<String, String> kafkaTemplate,
                                         ObjectMapper objectMapper,
                                         @Value("${app.kafka.topics.vehicle-location-events}") String vehicleLocationEventsTopic,
                                         @Value("${app.kafka.publish.max-attempts}") int maxAttempts,
                                         @Value("${app.kafka.publish.backoff-ms}") long backoffMs) {
        this.kafkaTemplate = kafkaTemplate;
        this.objectMapper = objectMapper;
        this.vehicleLocationEventsTopic = vehicleLocationEventsTopic;
        this.maxAttempts = maxAttempts;
        this.backoffMs = backoffMs;
    }

    public void publish(LocationUpdateRequest request, VehicleAssignmentSnapshot snapshot) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("schemaVersion", 1);
        payload.put("eventId", UUID.randomUUID().toString());
        payload.put("eventType", "VEHICLE_LOCATION_UPDATED");
        payload.put("occurredAt", Instant.now().toString());
        payload.put("source", "tracking-service");
        payload.put("vehicleId", request.getVehicleId());
        payload.put("driverId", request.getDriverId());
        payload.put("orderIds", snapshot.getOrderIds());
        payload.put("lat", request.getLat());
        payload.put("lng", request.getLng());
        payload.put("speed", request.getSpeed());
        payload.put("heading", request.getHeading());
        payload.put("timestamp", request.getTimestamp());
        send(request.getVehicleId(), payload);
    }

    private void send(Long vehicleId, Map<String, Object> payload) {
        try {
            String message = objectMapper.writeValueAsString(payload);
            int attempts = 0;
            while (attempts < maxAttempts) {
                attempts++;
                try {
                    kafkaTemplate.send(vehicleLocationEventsTopic, String.valueOf(vehicleId), message).get();
                    logger.info("Published VEHICLE_LOCATION_UPDATED for vehicle {}", vehicleId);
                    return;
                } catch (InterruptedException ex) {
                    Thread.currentThread().interrupt();
                    throw ex;
                } catch (ExecutionException ex) {
                    if (attempts >= maxAttempts) {
                        throw ex;
                    }
                    logger.warn("Retrying VEHICLE_LOCATION_UPDATED for vehicle {} after Kafka publish failure on attempt {}", vehicleId, attempts);
                    Thread.sleep(backoffMs);
                }
            }
        } catch (JsonProcessingException | InterruptedException | ExecutionException ex) {
            if (ex instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
            throw new RuntimeException("Failed to publish location event for vehicle " + vehicleId, ex);
        }
    }
}
