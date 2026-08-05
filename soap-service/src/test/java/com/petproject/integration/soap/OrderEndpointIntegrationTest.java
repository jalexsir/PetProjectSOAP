package com.petproject.integration.soap;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.ws.test.server.MockWebServiceClient;
import org.springframework.xml.transform.StringSource;

import java.util.Map;

import static org.springframework.ws.test.server.RequestCreators.withPayload;
import static org.springframework.ws.test.server.ResponseMatchers.clientOrSenderFault;
import static org.springframework.ws.test.server.ResponseMatchers.noFault;
import static org.springframework.ws.test.server.ResponseMatchers.xpath;

@SpringBootTest
class OrderEndpointIntegrationTest {

    private static final Map<String, String> NS =
            Map.of("t", "http://petproject.integration/orders");

    @Autowired
    private ApplicationContext applicationContext;

    private MockWebServiceClient mockClient;

    @BeforeEach
    void setUp() {
        mockClient = MockWebServiceClient.createClient(applicationContext);
    }

    @Test
    void getOrder_returnsExistingOrder() throws Exception {
        StringSource request = new StringSource(
                "<getOrderRequest xmlns='http://petproject.integration/orders'>"
                        + "<orderId>ORD-1001</orderId></getOrderRequest>");

        mockClient.sendRequest(withPayload(request))
                .andExpect(noFault())
                .andExpect(xpath("/t:getOrderResponse/t:order/t:orderId", NS).evaluatesTo("ORD-1001"))
                .andExpect(xpath("/t:getOrderResponse/t:order/t:customerId", NS).evaluatesTo("CUST-777"))
                .andExpect(xpath("/t:getOrderResponse/t:order/t:status", NS).evaluatesTo("NEW"));
    }

    @Test
    void getOrder_unknownId_returnsSoapFault() throws Exception {
        StringSource request = new StringSource(
                "<getOrderRequest xmlns='http://petproject.integration/orders'>"
                        + "<orderId>DOES-NOT-EXIST</orderId></getOrderRequest>");

        mockClient.sendRequest(withPayload(request))
                .andExpect(clientOrSenderFault("Order not found"));
    }

    @Test
    void createOrder_persistsAndConfirmsOrder() throws Exception {
        StringSource request = new StringSource(
                "<createOrderRequest xmlns='http://petproject.integration/orders'>"
                        + "<order>"
                        + "<orderId>ignored-by-server</orderId>"
                        + "<customerId>CUST-555</customerId>"
                        + "<status>NEW</status>"
                        + "<createdAt>2026-08-05T10:00:00Z</createdAt>"
                        + "<items><item><sku>SKU-1</sku><quantity>1</quantity><unitPrice>10.00</unitPrice></item></items>"
                        + "</order></createOrderRequest>");

        mockClient.sendRequest(withPayload(request))
                .andExpect(noFault())
                .andExpect(xpath("/t:createOrderResponse/t:status", NS).evaluatesTo("CONFIRMED"));
    }
}
