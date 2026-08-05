package com.petproject.integration.hub;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * "Міні-ESB": з'єднує SOAP-, REST- та MQTT-канали інтеграції в один процес
 * обробки замовлення (Apache Camel routes у IntegrationHubRoutes).
 */
@SpringBootApplication
public class IntegrationHubApplication {

    public static void main(String[] args) {
        SpringApplication.run(IntegrationHubApplication.class, args);
    }
}
