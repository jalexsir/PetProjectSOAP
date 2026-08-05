package com.petproject.integration.hub.routes;

import com.petproject.integration.hub.gateway.MqttChannelGateway;
import com.petproject.integration.hub.gateway.RestChannelGateway;
import com.petproject.integration.hub.gateway.SoapChannelGateway;
import com.petproject.integration.hub.processor.CrossDomainGuardProcessor;
import com.petproject.integration.hub.processor.JsonToXmlOrderTranslator;
import org.apache.camel.builder.RouteBuilder;
import org.springframework.stereotype.Component;

/**
 * "Міні-ESB": одна вхідна точка (canonical Order у JSON), яка після перевірки
 * на межі домену (CDS-guard) маршрутизується Content-Based Router'ом в один
 * із трьох каналів. SOAP-гілка додатково проходить Message Translator
 * (JSON -> XML), бо цільовий контракт — інший формат.
 *
 * Вхідна точка для тестів/оркестрації: direct:hub-inbound-order
 * (заголовок Camel-повідомлення "channel" = soap | rest | mqtt).
 * Вхідна точка для ручного тестування по HTTP: POST /hub/orders
 * (заголовок X-Channel визначає канал, platform-http:// прокидає його як Camel header).
 */
@Component
public class IntegrationHubRoutes extends RouteBuilder {

    private final CrossDomainGuardProcessor crossDomainGuardProcessor;
    private final JsonToXmlOrderTranslator jsonToXmlOrderTranslator;
    private final SoapChannelGateway soapChannelGateway;
    private final RestChannelGateway restChannelGateway;
    private final MqttChannelGateway mqttChannelGateway;

    public IntegrationHubRoutes(CrossDomainGuardProcessor crossDomainGuardProcessor,
                                 JsonToXmlOrderTranslator jsonToXmlOrderTranslator,
                                 SoapChannelGateway soapChannelGateway,
                                 RestChannelGateway restChannelGateway,
                                 MqttChannelGateway mqttChannelGateway) {
        this.crossDomainGuardProcessor = crossDomainGuardProcessor;
        this.jsonToXmlOrderTranslator = jsonToXmlOrderTranslator;
        this.soapChannelGateway = soapChannelGateway;
        this.restChannelGateway = restChannelGateway;
        this.mqttChannelGateway = mqttChannelGateway;
    }

    @Override
    public void configure() {

        // HTTP-адаптер: тонкий фронт для ручного curl-тестування (README).
        from("platform-http:/hub/orders?httpMethodRestrict=POST")
                .routeId("http-inbound-adapter")
                .setHeader("channel", simple("${header.X-Channel}"))
                .to("direct:hub-inbound-order");

        // Ядро ESB-логіки: CDS-guard -> Content-Based Router -> (Message Translator) -> протокольний адаптер.
        from("direct:hub-inbound-order")
                .routeId("esb-core")
                .log("ESB core received order, channel=${header.channel}")
                .process(crossDomainGuardProcessor)
                .choice()
                    .when(header("channel").isEqualTo("soap"))
                        .to("direct:route-to-soap")
                    .when(header("channel").isEqualTo("mqtt"))
                        .to("direct:route-to-mqtt")
                    .otherwise()
                        .to("direct:route-to-rest")
                .end();

        from("direct:route-to-soap")
                .routeId("route-to-soap")
                .process(jsonToXmlOrderTranslator)
                .log("Translated canonical order JSON -> XML, forwarding to SOAP channel")
                .bean(soapChannelGateway, "send");

        from("direct:route-to-rest")
                .routeId("route-to-rest")
                .log("Forwarding canonical order as-is to REST channel")
                .bean(restChannelGateway, "send");

        from("direct:route-to-mqtt")
                .routeId("route-to-mqtt")
                .log("Publishing canonical order event to MQTT channel")
                .bean(mqttChannelGateway, "send");
    }
}
