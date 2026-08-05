package com.petproject.integration.model;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * "Канонічна" (canonical data model) POJO-версія замовлення для JSON/REST-світу.
 * Семантично ідентична complexType tns:Order в order.xsd — навмисно, щоб
 * OrderMapper міг один-в-один перекладати XML <-> JSON (message translator EIP).
 */
public class Order {

    private String orderId;
    private String customerId;
    private OrderStatus status;
    private OffsetDateTime createdAt;
    private List<OrderItem> items = new ArrayList<>();

    public Order() {
    }

    public Order(String orderId, String customerId, OrderStatus status,
                 OffsetDateTime createdAt, List<OrderItem> items) {
        this.orderId = orderId;
        this.customerId = customerId;
        this.status = status;
        this.createdAt = createdAt;
        this.items = items;
    }

    public String getOrderId() {
        return orderId;
    }

    public void setOrderId(String orderId) {
        this.orderId = orderId;
    }

    public String getCustomerId() {
        return customerId;
    }

    public void setCustomerId(String customerId) {
        this.customerId = customerId;
    }

    public OrderStatus getStatus() {
        return status;
    }

    public void setStatus(OrderStatus status) {
        this.status = status;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(OffsetDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public List<OrderItem> getItems() {
        return items;
    }

    public void setItems(List<OrderItem> items) {
        this.items = items;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Order order)) return false;
        return Objects.equals(orderId, order.orderId)
                && Objects.equals(customerId, order.customerId)
                && status == order.status
                && Objects.equals(createdAt, order.createdAt)
                && Objects.equals(items, order.items);
    }

    @Override
    public int hashCode() {
        return Objects.hash(orderId, customerId, status, createdAt, items);
    }

    @Override
    public String toString() {
        return "Order{orderId='%s', customerId='%s', status=%s, createdAt=%s, items=%s}"
                .formatted(orderId, customerId, status, createdAt, items);
    }
}
