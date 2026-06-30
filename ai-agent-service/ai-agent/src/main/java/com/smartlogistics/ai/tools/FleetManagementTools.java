package com.smartlogistics.ai.tools;

import com.smartlogistics.ai.client.VehicleServiceFeignClient;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Description;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

@Configuration
@RequiredArgsConstructor
public class FleetManagementTools {

    private final VehicleServiceFeignClient vehicleClient;

    public record EmptyRequest() {}
    public record VehicleStatusRequest(Long vehicleId) {}

    @Bean
    @Description("Find all idle or available vehicles in the fleet")
    public Function<EmptyRequest, List<Map<String, Object>>> findIdleVehicles() {
        return request -> vehicleClient.findIdleVehicles().getBody();
    }

    @Bean
    @Description("Get current status and assigned driver for a specific vehicle by its ID")
    public Function<VehicleStatusRequest, Map<String, Object>> getVehicleStatus() {
        return request -> vehicleClient.getVehicleStatus(request.vehicleId()).getBody();
    }
}
