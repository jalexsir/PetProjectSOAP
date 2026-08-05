package com.petproject.integration.hub.gateway;

/** Адаптер вихідного SOAP-каналу (протокольний адаптер ESB до soap-service). */
public interface SoapChannelGateway {

    /** @param createOrderRequestXmlFragment XML-фрагмент &lt;createOrderRequest&gt;...&lt;/createOrderRequest&gt; без XML-декларації */
    void send(String createOrderRequestXmlFragment);
}
