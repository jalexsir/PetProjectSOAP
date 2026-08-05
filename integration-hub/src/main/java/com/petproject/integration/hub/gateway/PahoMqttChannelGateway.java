package com.petproject.integration.hub.gateway;

import jakarta.annotation.PreDestroy;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.UUID;

/** Реальна публікація в MQTT-канал (той самий топік, який слухає mqtt-integration). */
@Component
public class PahoMqttChannelGateway implements MqttChannelGateway {

    private static final Logger LOGGER = LoggerFactory.getLogger(PahoMqttChannelGateway.class);

    private final String brokerUrl;
    private final String topic;
    private MqttClient client;

    public PahoMqttChannelGateway(
            @Value("${hub.mqtt.broker-url:tcp://localhost:1883}") String brokerUrl,
            @Value("${hub.mqtt.topic:integration/orders/events}") String topic) {
        this.brokerUrl = brokerUrl;
        this.topic = topic;
    }

    @Override
    public synchronized void send(String orderEventJson) {
        try {
            if (client == null || !client.isConnected()) {
                client = new MqttClient(brokerUrl, "integration-hub-" + UUID.randomUUID(), new MemoryPersistence());
                MqttConnectOptions options = new MqttConnectOptions();
                options.setAutomaticReconnect(true);
                options.setCleanSession(true);
                client.connect(options);
            }
            MqttMessage message = new MqttMessage(orderEventJson.getBytes());
            message.setQos(1);
            client.publish(topic, message);
            LOGGER.info("MQTT channel gateway -> topic '{}' on {}", topic, brokerUrl);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to publish order event to MQTT channel: " + brokerUrl, e);
        }
    }

    @PreDestroy
    public void shutdown() throws Exception {
        if (client != null && client.isConnected()) {
            client.disconnect();
            client.close();
        }
    }
}
