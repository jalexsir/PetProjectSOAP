package com.petproject.integration.mqtt.model;

import java.time.OffsetDateTime;

/**
 * Легка подія (не весь Order) — типовий підхід для MQTT-каналу: публікується
 * "сталася зміна", а деталі при потребі підтягуються синхронним викликом
 * (REST/SOAP). Це відрізняє асинхронний MQTT-канал від синхронних SOAP/REST.
 */
public record OrderEvent(String orderId, String eventType, String status, OffsetDateTime occurredAt) {

    public static OrderEvent created(String orderId, String status) {
        return new OrderEvent(orderId, "ORDER_CREATED", status, OffsetDateTime.now());
    }
}
