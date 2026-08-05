package com.petproject.integration.mqtt;

import com.petproject.integration.mqtt.model.OrderEvent;
import com.petproject.integration.mqtt.publisher.OrderEventPublisher;
import com.petproject.integration.mqtt.subscriber.OrderEventSubscriber;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static java.time.Duration.ofSeconds;

@SpringBootTest
@TestPropertySource(properties = {
        "mqtt.broker.port=18830",
        "mqtt.broker.url=tcp://localhost:18830"
})
class MqttPubSubIntegrationTest {

    @Autowired
    private OrderEventPublisher publisher;

    @Autowired
    private OrderEventSubscriber subscriber;

    @Test
    void publishedEvent_isDeliveredToSubscriber() {
        OrderEvent event = OrderEvent.created("ORD-TEST-1", "NEW");

        publisher.publish(event);

        await().atMost(ofSeconds(5)).untilAsserted(() -> {
            List<OrderEvent> received = subscriber.getReceivedEvents();
            assertThat(received).anyMatch(e -> e.orderId().equals("ORD-TEST-1"));
        });
    }
}
