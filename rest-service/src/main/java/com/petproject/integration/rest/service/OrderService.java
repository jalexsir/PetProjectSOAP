package com.petproject.integration.rest.service;

import com.petproject.integration.model.Order;
import com.petproject.integration.model.OrderItem;
import com.petproject.integration.model.OrderStatus;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class OrderService {

    private final Map<String, Order> orders = new ConcurrentHashMap<>();

    public OrderService() {
        Order sample = new Order();
        sample.setOrderId("ORD-2001");
        sample.setCustomerId("CUST-42");
        sample.setStatus(OrderStatus.NEW);
        sample.setCreatedAt(OffsetDateTime.now());
        sample.setItems(List.of(new OrderItem("SKU-MOUSE-01", 1, new BigDecimal("19.90"))));
        orders.put(sample.getOrderId(), sample);
    }

    public Optional<Order> findById(String orderId) {
        return Optional.ofNullable(orders.get(orderId));
    }

    public Order create(Order incoming) {
        incoming.setOrderId("ORD-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase());
        incoming.setStatus(OrderStatus.CONFIRMED);
        incoming.setCreatedAt(OffsetDateTime.now());
        orders.put(incoming.getOrderId(), incoming);
        return incoming;
    }
}
