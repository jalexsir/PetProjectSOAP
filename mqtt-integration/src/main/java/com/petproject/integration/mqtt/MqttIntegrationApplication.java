package com.petproject.integration.mqtt;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * MQTT-канал інтеграції: асинхронний pub/sub на противагу синхронним
 * SOAP (soap-service) та REST (rest-service).
 */
@SpringBootApplication
public class MqttIntegrationApplication {

    public static void main(String[] args) {
        SpringApplication.run(MqttIntegrationApplication.class, args);
    }
}
