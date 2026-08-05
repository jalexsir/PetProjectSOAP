package com.petproject.integration.soap;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * SOAP-канал інтеграції: contract-first веб-сервіс на Spring-WS.
 * WSDL: http://localhost:8081/ws/orders.wsdl
 */
@SpringBootApplication
public class SoapServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(SoapServiceApplication.class, args);
    }
}
