package com.smartlogistics.ai.tools;

import com.smartlogistics.ai.client.TrackingServiceFeignClient;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Description;
import java.util.Map;
import java.util.function.Function;

@Configuration
@RequiredArgsConstructor
public class TrackingTools {

    private final TrackingServiceFeignClient trackingClient;

    public record LiveLocationRequest(Long vehicleId) {}

    @Bean
    @Description("Get live location and tracking telemetry for a specific vehicle by ID")
    public Function<LiveLocationRequest, Map<String, Object>> getLiveLocation() {
        return request -> trackingClient.getLiveLocation(request.vehicleId()).getBody();
    }
}
