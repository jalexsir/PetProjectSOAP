package com.petproject.integration.mqtt.subscriber;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.petproject.integration.mqtt.model.OrderEvent;
import com.petproject.integration.mqtt.publisher.OrderEventPublisher;
import jakarta.annotation.PreDestroy;
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttCallback;
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

import java.util.List;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Симулює downstream-систему, яка асинхронно споживає інтеграційні події
 * (наприклад, окремий мікросервіс сповіщень чи склад). На відміну від SOAP/REST,
 * тут немає прямого запит-відповідь зв'язку — видавець (publisher) і споживач
 * (subscriber) повністю розв'язані (decoupled) через брокер.
 */
@Component
@DependsOn("embeddedMqttBroker")
public class OrderEventSubscriber {

    private static final Logger LOGGER = LoggerFactory.getLogger(OrderEventSubscriber.class);
    private static final int QOS_AT_LEAST_ONCE = 1;

    private final MqttClient client;
    private final List<OrderEvent> receivedEvents = new CopyOnWriteArrayList<>();

    public OrderEventSubscriber(@Value("${mqtt.broker.url:tcp://localhost:1883}") String brokerUrl,
                                 ObjectMapper objectMapper) throws MqttException {
        this.client = new MqttClient(brokerUrl, "order-event-subscriber-" + UUID.randomUUID(), new MemoryPersistence());

        client.setCallback(new MqttCallback() {
            @Override
            public void connectionLost(Throwable cause) {
                LOGGER.warn("MQTT connection lost", cause);
            }

            @Override
            public void messageArrived(String topic, MqttMessage message) throws Exception {
                OrderEvent event = objectMapper.readValue(message.getPayload(), OrderEvent.class);
                receivedEvents.add(event);
                LOGGER.info("Downstream consumer received event from '{}': {}", topic, event);
            }

            @Override
            public void deliveryComplete(IMqttDeliveryToken token) {
                // producer-side callback, not used by a pure subscriber
            }
        });

        MqttConnectOptions options = new MqttConnectOptions();
        options.setAutomaticReconnect(true);
        options.setCleanSession(true);
        client.connect(options);
        client.subscribe(OrderEventPublisher.TOPIC, QOS_AT_LEAST_ONCE);
    }

    public List<OrderEvent> getReceivedEvents() {
        return List.copyOf(receivedEvents);
    }

    @PreDestroy
    public void shutdown() throws MqttException {
        if (client.isConnected()) {
            client.disconnect();
        }
        client.close();
    }
}
