package com.smartlogistics.analytics.listener;

import com.smartlogistics.analytics.service.AnalyticsEventProcessor;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
public class OrderEventsListener {
    private final AnalyticsEventProcessor analyticsEventProcessor;

    public OrderEventsListener(AnalyticsEventProcessor analyticsEventProcessor) {
        this.analyticsEventProcessor = analyticsEventProcessor;
    }

    @KafkaListener(topics = "${app.kafka.topics.order-events}", groupId = "${spring.application.name}-order-events")
    public void consume(String payload) {
        analyticsEventProcessor.processOrderEvent(payload);
    }
}
