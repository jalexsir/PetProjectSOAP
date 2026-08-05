package com.petproject.integration.mqtt;

import com.petproject.integration.mqtt.model.OrderEvent;
import com.petproject.integration.mqtt.publisher.OrderEventPublisher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
public class MqttDemoRunner implements CommandLineRunner {

    private static final Logger LOGGER = LoggerFactory.getLogger(MqttDemoRunner.class);

    private final OrderEventPublisher publisher;

    public MqttDemoRunner(OrderEventPublisher publisher) {
        this.publisher = publisher;
    }

    @Override
    public void run(String... args) {
        LOGGER.info("--- MQTT demo: publishing a sample OrderEvent to '{}' ---", OrderEventPublisher.TOPIC);
        publisher.publish(OrderEvent.created("ORD-3001", "NEW"));
    }
}
