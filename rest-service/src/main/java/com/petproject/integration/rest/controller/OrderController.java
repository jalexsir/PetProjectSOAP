package com.petproject.integration.rest.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.petproject.integration.model.Order;
import com.petproject.integration.rest.service.OrderService;
import com.petproject.integration.validation.JsonSchemaValidator;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Три ідентичні за бізнес-логікою групи ендпоінтів, що відрізняються ЛИШЕ
 * механізмом автентифікації (правила по кожній групі — у SecurityConfig):
 *   /api/basic/orders  — HTTP Basic
 *   /api/oauth/orders  — OAuth2 / JWT Bearer
 *   /api/mtls/orders   — mTLS (клієнтський сертифікат, дивись rest-service мТLS-конектор)
 * POST-запит валідується проти order-schema.json ДО десеріалізації —
 * той самий принцип, що XSD-валідація на SOAP-каналі.
 */
@RestController
public class OrderController {

    private final OrderService orderService;
    private final JsonSchemaValidator orderSchemaValidator;
    private final ObjectMapper objectMapper;

    public OrderController(OrderService orderService,
                            JsonSchemaValidator orderSchemaValidator,
                            ObjectMapper objectMapper) {
        this.orderService = orderService;
        this.orderSchemaValidator = orderSchemaValidator;
        this.objectMapper = objectMapper;
    }

    @GetMapping("/api/basic/orders/{orderId}")
    public ResponseEntity<Order> getOrderBasic(@PathVariable String orderId) {
        return getOrder(orderId);
    }

    @PostMapping(value = "/api/basic/orders", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Order> createOrderBasic(@RequestBody String rawJson) {
        return createOrder(rawJson);
    }

    @GetMapping("/api/oauth/orders/{orderId}")
    public ResponseEntity<Order> getOrderOauth(@PathVariable String orderId) {
        return getOrder(orderId);
    }

    @PostMapping(value = "/api/oauth/orders", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Order> createOrderOauth(@RequestBody String rawJson) {
        return createOrder(rawJson);
    }

    @GetMapping("/api/mtls/orders/{orderId}")
    public ResponseEntity<Order> getOrderMtls(@PathVariable String orderId) {
        return getOrder(orderId);
    }

    @PostMapping(value = "/api/mtls/orders", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Order> createOrderMtls(@RequestBody String rawJson) {
        return createOrder(rawJson);
    }

    private ResponseEntity<Order> getOrder(String orderId) {
        return orderService.findById(orderId)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    private ResponseEntity<Order> createOrder(String rawJson) {
        List<String> errors = orderSchemaValidator.validate(rawJson);
        if (!errors.isEmpty()) {
            throw new JsonSchemaValidationException(errors);
        }
        try {
            Order order = objectMapper.readValue(rawJson, Order.class);
            Order saved = orderService.create(order);
            return ResponseEntity.status(HttpStatus.CREATED).body(saved);
        } catch (JsonProcessingException e) {
            throw new JsonSchemaValidationException(List.of("Malformed JSON: " + e.getOriginalMessage()));
        }
    }
}
