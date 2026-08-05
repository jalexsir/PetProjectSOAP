package com.petproject.integration.soap.endpoint;

import org.springframework.ws.soap.server.endpoint.annotation.FaultCode;
import org.springframework.ws.soap.server.endpoint.annotation.SoapFault;

/**
 * @SoapFault перетворює цей виняток у справжній SOAP Fault (<soap:Fault>)
 * замість HTTP 500 — саме так контракт SOAP-сервісу описує помилкові сценарії.
 */
@SoapFault(faultCode = FaultCode.CLIENT, faultStringOrReason = "Order not found")
public class OrderNotFoundException extends RuntimeException {

    public OrderNotFoundException(String orderId) {
        super("Order not found: " + orderId);
    }
}
