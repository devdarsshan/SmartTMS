package com.smartlogistics.analytics.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.listener.CommonErrorHandler;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.util.backoff.FixedBackOff;

@Configuration
public class KafkaConfig {
    private static final Logger logger = LoggerFactory.getLogger(KafkaConfig.class);

    @Bean
    public CommonErrorHandler kafkaErrorHandler(@Value("${app.kafka.consumer.backoff-ms}") long backoffMs,
                                                @Value("${app.kafka.consumer.retry-attempts}") long retryAttempts) {
        return new DefaultErrorHandler(
                (record, ex) -> logger.error("Skipping Kafka record after retries on topic {}: {}", record.topic(), ex.getMessage(), ex),
                new FixedBackOff(backoffMs, retryAttempts)
        );
    }
}
