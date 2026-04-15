package com.smartlogistics.tracking.service;

import com.smartlogistics.tracking.dto.LiveVehicleLocation;
import com.smartlogistics.tracking.dto.LocationUpdateRequest;
import com.smartlogistics.tracking.dto.OrderDTO;
import com.smartlogistics.tracking.dto.OrderTrackingSummaryResponse;
import com.smartlogistics.tracking.dto.UserProfileDTO;
import com.smartlogistics.tracking.events.VehicleLocationEventPublisher;
import com.smartlogistics.tracking.exceptions.ResourceNotFoundException;
import com.smartlogistics.tracking.exceptions.TrackingValidationException;
import com.smartlogistics.tracking.exceptions.UnauthorizedOperationException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;

@Service
public class TrackingService {
    private final RedisTemplate<String, Object> redisTemplate;
    private final VehicleAssignmentCacheService vehicleAssignmentCacheService;
    private final UserServiceClient userServiceClient;
    private final OrderServiceClient orderServiceClient;
    private final VehicleLocationEventPublisher vehicleLocationEventPublisher;
    private final long locationTtlSeconds;
    private final long maxAgeSeconds;

    public TrackingService(RedisTemplate<String, Object> redisTemplate,
                           VehicleAssignmentCacheService vehicleAssignmentCacheService,
                           UserServiceClient userServiceClient,
                           OrderServiceClient orderServiceClient,
                           VehicleLocationEventPublisher vehicleLocationEventPublisher,
                           @Value("${app.tracking.location.ttl-seconds}") long locationTtlSeconds,
                           @Value("${app.tracking.timestamp.max-age-seconds}") long maxAgeSeconds) {
        this.redisTemplate = redisTemplate;
        this.vehicleAssignmentCacheService = vehicleAssignmentCacheService;
        this.userServiceClient = userServiceClient;
        this.orderServiceClient = orderServiceClient;
        this.vehicleLocationEventPublisher = vehicleLocationEventPublisher;
        this.locationTtlSeconds = locationTtlSeconds;
        this.maxAgeSeconds = maxAgeSeconds;
    }

    public void ingestLocation(LocationUpdateRequest request, String userIdHeader) {
        Long authUserId = parseAuthUserId(userIdHeader);
        if (!authUserId.equals(request.getDriverId())) {
            throw new UnauthorizedOperationException("Drivers may only submit tracking for themselves");
        }

        UserProfileDTO userProfile = userServiceClient.fetchUserProfile(authUserId);
        if (userProfile.getUserRole() == null || !userProfile.getUserRole().equalsIgnoreCase("DRIVER")) {
            throw new UnauthorizedOperationException("Only DRIVER users can submit tracking updates");
        }

        validateTimestampFreshness(request.getTimestamp());

        VehicleAssignmentSnapshot snapshot = vehicleAssignmentCacheService.resolveVehicle(request.getVehicleId());
        if (snapshot.getDriverId() == null || !snapshot.getDriverId().equals(request.getDriverId())) {
            throw new TrackingValidationException("Vehicle and driver linkage is invalid for tracking update");
        }

        LiveVehicleLocation liveVehicleLocation = new LiveVehicleLocation();
        liveVehicleLocation.setVehicleId(request.getVehicleId());
        liveVehicleLocation.setDriverId(request.getDriverId());
        liveVehicleLocation.setLat(request.getLat());
        liveVehicleLocation.setLng(request.getLng());
        liveVehicleLocation.setSpeed(request.getSpeed());
        liveVehicleLocation.setHeading(request.getHeading());
        liveVehicleLocation.setTimestamp(request.getTimestamp());
        liveVehicleLocation.setLastUpdatedAt(Instant.ofEpochSecond(request.getTimestamp()).toString());
        liveVehicleLocation.setStatus("LIVE");
        liveVehicleLocation.setOrderIds(snapshot.getOrderIds());

        String redisKey = buildRedisKey(request.getVehicleId());
        redisTemplate.opsForValue().set(redisKey, liveVehicleLocation, Duration.ofSeconds(locationTtlSeconds));

        try {
            vehicleLocationEventPublisher.publish(request, snapshot);
        } catch (RuntimeException ex) {
            redisTemplate.delete(redisKey);
            throw ex;
        }
    }

    public LiveVehicleLocation fetchLatestVehicleLocation(Long vehicleId) {
        Object value = redisTemplate.opsForValue().get(buildRedisKey(vehicleId));
        if (value == null) {
            throw new ResourceNotFoundException("No live location found for vehicle " + vehicleId);
        }
        return (LiveVehicleLocation) value;
    }

    public OrderTrackingSummaryResponse fetchOrderTracking(Long orderId) {
        OrderDTO order = orderServiceClient.fetchOrder(orderId);

        OrderTrackingSummaryResponse response = new OrderTrackingSummaryResponse();
        response.setOrderId(order.getOrderId());
        response.setOrderStatus(order.getStatus());
        response.setVehicleId(order.getVehicleId());

        if (order.getVehicleId() != null) {
            Object liveLocation = redisTemplate.opsForValue().get(buildRedisKey(order.getVehicleId()));
            if (liveLocation instanceof LiveVehicleLocation location) {
                response.setLatestLocation(location);
                response.setLastUpdatedAt(location.getLastUpdatedAt());
            }
        }

        return response;
    }

    private void validateTimestampFreshness(Long epochSeconds) {
        Instant timestamp = Instant.ofEpochSecond(epochSeconds);
        Instant now = Instant.now();
        if (timestamp.isBefore(now.minusSeconds(maxAgeSeconds)) || timestamp.isAfter(now.plusSeconds(locationTtlSeconds))) {
            throw new TrackingValidationException("Tracking timestamp is stale or too far in the future");
        }
    }

    private Long parseAuthUserId(String userIdHeader) {
        if (userIdHeader == null || userIdHeader.trim().isEmpty()) {
            throw new UnauthorizedOperationException("X-User-Id header is required");
        }
        try {
            return Long.parseLong(userIdHeader);
        } catch (NumberFormatException ex) {
            throw new UnauthorizedOperationException("X-User-Id header must be a valid number");
        }
    }

    private String buildRedisKey(Long vehicleId) {
        return "live:vehicle:" + vehicleId;
    }
}
