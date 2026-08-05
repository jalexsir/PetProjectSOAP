package com.petproject.integration.hub.gateway;

/** Адаптер вихідного MQTT-каналу (протокольний адаптер ESB до брокера подій). */
public interface MqttChannelGateway {

    void send(String orderEventJson);
}
