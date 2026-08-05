package com.petproject.integration.hub;

import com.petproject.integration.hub.exception.CrossDomainViolationException;
import com.petproject.integration.hub.gateway.MqttChannelGateway;
import com.petproject.integration.hub.gateway.RestChannelGateway;
import com.petproject.integration.hub.gateway.SoapChannelGateway;
import org.apache.camel.CamelExecutionException;
import org.apache.camel.ProducerTemplate;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.Mockito.verify;

@SpringBootTest
class IntegrationHubRoutesTest {

    private static final String VALID_ORDER_JSON = "{"
            + "\"orderId\":\"ORD-9001\",\"customerId\":\"CUST-9\",\"status\":\"NEW\","
            + "\"createdAt\":\"2026-08-05T10:00:00Z\","
            + "\"items\":[{\"sku\":\"SKU-1\",\"quantity\":2,\"unitPrice\":9.99}]}";

    private static final String INVALID_ORDER_JSON = "{\"orderId\":\"ORD-9002\"}"; // без обов'язкових полів

    @Autowired
    private ProducerTemplate producerTemplate;

    @MockBean
    private SoapChannelGateway soapChannelGateway;

    @MockBean
    private RestChannelGateway restChannelGateway;

    @MockBean
    private MqttChannelGateway mqttChannelGateway;

    @Test
    void soapChannel_translatesJsonToXmlAndCallsSoapGateway() {
        producerTemplate.sendBodyAndHeader("direct:hub-inbound-order", VALID_ORDER_JSON, "channel", "soap");

        verify(soapChannelGateway).send(contains("<createOrderRequest"));
    }

    @Test
    void restChannel_forwardsJsonAsIsToRestGateway() {
        producerTemplate.sendBodyAndHeader("direct:hub-inbound-order", VALID_ORDER_JSON, "channel", "rest");

        verify(restChannelGateway).send(contains("ORD-9001"));
    }

    @Test
    void mqttChannel_forwardsEventToMqttGateway() {
        producerTemplate.sendBodyAndHeader("direct:hub-inbound-order", VALID_ORDER_JSON, "channel", "mqtt");

        verify(mqttChannelGateway).send(contains("ORD-9001"));
    }

    @Test
    void crossDomainGuard_rejectsMessageViolatingSchema() {
        assertThatThrownBy(() ->
                producerTemplate.sendBodyAndHeader("direct:hub-inbound-order", INVALID_ORDER_JSON, "channel", "rest"))
                .isInstanceOf(CamelExecutionException.class)
                .rootCause()
                .isInstanceOf(CrossDomainViolationException.class);

        verify(restChannelGateway, org.mockito.Mockito.never()).send(any());
    }
}
