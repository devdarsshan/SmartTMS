package com.smartlogistics.analytics.listener;

import com.smartlogistics.analytics.service.AnalyticsEventProcessor;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
public class VehicleEventsListener {
    private final AnalyticsEventProcessor analyticsEventProcessor;

    public VehicleEventsListener(AnalyticsEventProcessor analyticsEventProcessor) {
        this.analyticsEventProcessor = analyticsEventProcessor;
    }

    @KafkaListener(topics = "${app.kafka.topics.vehicle-events}", groupId = "${spring.application.name}-vehicle-events")
    public void consume(String payload) {
        analyticsEventProcessor.processVehicleEvent(payload);
    }
}
