package com.petproject.integration.mqtt.publisher;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.petproject.integration.mqtt.model.OrderEvent;
import jakarta.annotation.PreDestroy;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.DependsOn;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * Публікує події про замовлення в MQTT-топік. QoS 1 (at-least-once) —
 * типовий вибір для інтеграційних подій: втрата події неприпустима,
 * а дублікат обробляється споживачем ідемпотентно.
 */
@Component
@DependsOn("embeddedMqttBroker")
public class OrderEventPublisher {

    private static final Logger LOGGER = LoggerFactory.getLogger(OrderEventPublisher.class);
    public static final String TOPIC = "integration/orders/events";
    private static final int QOS_AT_LEAST_ONCE = 1;

    private final MqttClient client;
    private final ObjectMapper objectMapper;

    public OrderEventPublisher(@Value("${mqtt.broker.url:tcp://localhost:1883}") String brokerUrl,
                                ObjectMapper objectMapper) throws MqttException {
        this.objectMapper = objectMapper;
        this.client = new MqttClient(brokerUrl, "order-event-publisher-" + UUID.randomUUID(), new MemoryPersistence());

        MqttConnectOptions options = new MqttConnectOptions();
        options.setAutomaticReconnect(true);
        options.setCleanSession(true);
        client.connect(options);
    }

    public void publish(OrderEvent event) {
        try {
            byte[] payload = objectMapper.writeValueAsBytes(event);
            MqttMessage message = new MqttMessage(payload);
            message.setQos(QOS_AT_LEAST_ONCE);
            client.publish(TOPIC, message);
            LOGGER.info("Published MQTT event to '{}': {}", TOPIC, event);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to publish MQTT order event", e);
        }
    }

    @PreDestroy
    public void shutdown() throws MqttException {
        if (client.isConnected()) {
            client.disconnect();
        }
        client.close();
    }
}
