package com.smartlogistics.ai.tools;

import com.smartlogistics.ai.client.OrderServiceFeignClient;
import com.smartlogistics.ai.client.VehicleServiceFeignClient;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Description;
import java.util.Map;
import java.util.function.Function;

@Configuration
@RequiredArgsConstructor
public class OrderDispatchTools {

    private final OrderServiceFeignClient orderClient;
    private final VehicleServiceFeignClient vehicleClient;

    public record OrderRequest(Long orderId) {}
    public record AssignOrderRequest(Long orderId, Long vehicleId) {}

    @Bean
    @Description("Get details about a specific order by its ID")
    public Function<OrderRequest, Map<String, Object>> getOrderDetails() {
        return request -> orderClient.getOrderDetails(request.orderId()).getBody();
    }

    @Bean
    @Description("Assign a specific order to a vehicle")
    public Function<AssignOrderRequest, Map<String, Object>> assignOrderToVehicle() {
        return request -> vehicleClient.assignOrderToVehicle(request.vehicleId(), request.orderId()).getBody();
    }
}
