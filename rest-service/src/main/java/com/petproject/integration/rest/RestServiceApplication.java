package com.petproject.integration.rest;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * REST/JSON-канал інтеграції. Три групи ендпоінтів, кожна зі своїм механізмом
 * автентифікації (порівняти з SOAP-каналом, де застосовано лише Basic):
 *   /api/basic/**  -> HTTP Basic
 *   /api/oauth/**  -> OAuth2 / JWT (Bearer)
 *   /api/mtls/**   -> mTLS (клієнтський X.509-сертифікат)
 * Дефолтний профіль піднімає звичайний HTTP-конектор (щоб basic/oauth демо
 * працювало "з коробки"); профіль "mtls" додатково вмикає TLS-конектор
 * з client-auth=want (див. application-mtls.yml та certs/generate-certs.sh).
 */
@SpringBootApplication
public class RestServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(RestServiceApplication.class, args);
    }
}
