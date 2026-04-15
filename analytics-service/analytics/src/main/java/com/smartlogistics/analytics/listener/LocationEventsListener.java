package com.smartlogistics.analytics.listener;

import com.smartlogistics.analytics.service.AnalyticsEventProcessor;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
public class LocationEventsListener {
    private final AnalyticsEventProcessor analyticsEventProcessor;

    public LocationEventsListener(AnalyticsEventProcessor analyticsEventProcessor) {
        this.analyticsEventProcessor = analyticsEventProcessor;
    }

    @KafkaListener(topics = "${app.kafka.topics.vehicle-location-events}", groupId = "${spring.application.name}-location-events")
    public void consume(String payload) {
        analyticsEventProcessor.processLocationEvent(payload);
    }
}
