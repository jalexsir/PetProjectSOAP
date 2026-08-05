package com.petproject.integration.hub.gateway;

/** Адаптер вихідного REST-каналу (протокольний адаптер ESB до rest-service). */
public interface RestChannelGateway {

    void send(String orderJson);
}
