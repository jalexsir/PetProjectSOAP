package com.petproject.integration.model;

/**
 * Дзеркалить simpleType tns:OrderStatus з order.xsd — той самий домен значень,
 * представлений і в XML (XSD enumeration), і в JSON (JSON Schema enum).
 */
public enum OrderStatus {
    NEW,
    CONFIRMED,
    SHIPPED,
    CANCELLED
}
