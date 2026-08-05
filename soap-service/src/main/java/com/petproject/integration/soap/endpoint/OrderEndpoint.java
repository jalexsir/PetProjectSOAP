package com.petproject.integration.soap.endpoint;

import com.petproject.integration.soap.service.OrderStore;
import com.petproject.integration.xml.generated.CreateOrderRequest;
import com.petproject.integration.xml.generated.CreateOrderResponse;
import com.petproject.integration.xml.generated.GetOrderRequest;
import com.petproject.integration.xml.generated.GetOrderResponse;
import com.petproject.integration.xml.generated.Order;
import org.springframework.ws.server.endpoint.annotation.Endpoint;
import org.springframework.ws.server.endpoint.annotation.PayloadRoot;
import org.springframework.ws.server.endpoint.annotation.RequestPayload;
import org.springframework.ws.server.endpoint.annotation.ResponsePayload;

/**
 * SOAP-ендпоінт: маршрутизація за (namespace, local part) кореневого елемента
 * тіла конверта — саме так Spring-WS реалізує диспетчеризацію без потреби
 * в JAX-WS Application Server (@PayloadRoot замість javax.jws.WebMethod).
 */
@Endpoint
public class OrderEndpoint {

    private static final String NAMESPACE_URI = "http://petproject.integration/orders";

    private final OrderStore orderStore;

    public OrderEndpoint(OrderStore orderStore) {
        this.orderStore = orderStore;
    }

    @PayloadRoot(namespace = NAMESPACE_URI, localPart = "getOrderRequest")
    @ResponsePayload
    public GetOrderResponse getOrder(@RequestPayload GetOrderRequest request) {
        Order order = orderStore.findById(request.getOrderId())
                .orElseThrow(() -> new OrderNotFoundException(request.getOrderId()));

        GetOrderResponse response = new GetOrderResponse();
        response.setOrder(order);
        return response;
    }

    @PayloadRoot(namespace = NAMESPACE_URI, localPart = "createOrderRequest")
    @ResponsePayload
    public CreateOrderResponse createOrder(@RequestPayload CreateOrderRequest request) {
        Order saved = orderStore.save(request.getOrder());

        CreateOrderResponse response = new CreateOrderResponse();
        response.setOrderId(saved.getOrderId());
        response.setStatus(saved.getStatus());
        return response;
    }
}
