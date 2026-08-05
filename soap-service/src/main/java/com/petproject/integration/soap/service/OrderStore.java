package com.petproject.integration.soap.service;

import com.petproject.integration.xml.generated.Order;
import com.petproject.integration.xml.generated.OrderItem;
import com.petproject.integration.xml.generated.OrderItems;
import com.petproject.integration.xml.generated.OrderStatus;
import org.springframework.stereotype.Component;

import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.DatatypeFactory;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.GregorianCalendar;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * In-memory "система замовлень", яку інтегрує SOAP-канал. У реальному проєкті
 * тут був би виклик до бекенда (наприклад, SAP через RFC/IDoc, або БД) —
 * для pet-проєкту досить мапи в пам'яті, суть демонстрації протоколу від цього не змінюється.
 */
@Component
public class OrderStore {

    private final Map<String, Order> orders = new ConcurrentHashMap<>();
    private final DatatypeFactory datatypeFactory;

    public OrderStore() {
        try {
            this.datatypeFactory = DatatypeFactory.newInstance();
        } catch (DatatypeConfigurationException e) {
            throw new IllegalStateException(e);
        }
        seed();
    }

    public Optional<Order> findById(String orderId) {
        return Optional.ofNullable(orders.get(orderId));
    }

    public Order save(Order order) {
        if (order.getOrderId() == null || order.getOrderId().isBlank()) {
            order.setOrderId("ORD-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase());
        }
        order.setStatus(OrderStatus.CONFIRMED);
        orders.put(order.getOrderId(), order);
        return order;
    }

    private void seed() {
        Order sample = new Order();
        sample.setOrderId("ORD-1001");
        sample.setCustomerId("CUST-777");
        sample.setStatus(OrderStatus.NEW);
        sample.setCreatedAt(datatypeFactory.newXMLGregorianCalendar(new GregorianCalendar()));

        OrderItem item = new OrderItem();
        item.setSku("SKU-KEYBOARD-01");
        item.setQuantity(BigInteger.valueOf(2));
        item.setUnitPrice(new BigDecimal("39.90"));

        OrderItems items = new OrderItems();
        items.getItem().add(item);
        sample.setItems(items);

        orders.put(sample.getOrderId(), sample);
    }
}
