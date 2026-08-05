package com.petproject.integration.rest;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.DEFINED_PORT)
class RestServiceIntegrationTest {

    @Autowired
    private TestRestTemplate restTemplate;

    @Test
    void getOrder_withoutCredentials_isUnauthorized() {
        ResponseEntity<String> response = restTemplate.getForEntity("/api/basic/orders/ORD-2001", String.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void getOrder_withBasicAuth_returnsSeededOrder() {
        HttpHeaders headers = new HttpHeaders();
        headers.setBasicAuth("integration", "integration-pass");
        ResponseEntity<String> response = restTemplate.exchange(
                "/api/basic/orders/ORD-2001", org.springframework.http.HttpMethod.GET,
                new HttpEntity<>(headers), String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).contains("CUST-42");
    }

    @Test
    void createOrder_withInvalidPayload_isRejectedByJsonSchema() {
        HttpHeaders headers = new HttpHeaders();
        headers.setBasicAuth("integration", "integration-pass");
        headers.setContentType(MediaType.APPLICATION_JSON);
        // бракує обов'язкового поля "items" -> має впасти на JSON Schema валідації
        String invalidJson = "{\"orderId\":\"x\",\"customerId\":\"c\",\"status\":\"NEW\",\"createdAt\":\"2026-08-05T10:00:00Z\"}";

        ResponseEntity<String> response = restTemplate.exchange(
                "/api/basic/orders", org.springframework.http.HttpMethod.POST,
                new HttpEntity<>(invalidJson, headers), String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).contains("schema_validation_failed");
    }

    @Test
    void oauthFlow_tokenIssuedByMockAuthServer_grantsAccessToOrders() {
        ResponseEntity<Map> tokenResponse = restTemplate.postForEntity(
                "/api/public/auth/token?subject=test-client&scope=orders.read", null, Map.class);
        assertThat(tokenResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        String accessToken = (String) tokenResponse.getBody().get("access_token");
        assertThat(accessToken).isNotBlank();

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(accessToken);
        ResponseEntity<String> ordersResponse = restTemplate.exchange(
                "/api/oauth/orders/ORD-2001", org.springframework.http.HttpMethod.GET,
                new HttpEntity<>(headers), String.class);

        assertThat(ordersResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(ordersResponse.getBody()).contains("CUST-42");
    }

    @Test
    void oauthEndpoint_withReadOnlyScope_rejectsWriteOperation() {
        ResponseEntity<Map> tokenResponse = restTemplate.postForEntity(
                "/api/public/auth/token?subject=test-client&scope=orders.read", null, Map.class);
        String accessToken = (String) tokenResponse.getBody().get("access_token");

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(accessToken);
        headers.setContentType(MediaType.APPLICATION_JSON);
        String validJson = "{\"orderId\":\"ignored\",\"customerId\":\"c\",\"status\":\"NEW\"," +
                "\"createdAt\":\"2026-08-05T10:00:00Z\",\"items\":[{\"sku\":\"SKU-1\",\"quantity\":1,\"unitPrice\":5.0}]}";

        ResponseEntity<String> response = restTemplate.exchange(
                "/api/oauth/orders", org.springframework.http.HttpMethod.POST,
                new HttpEntity<>(validJson, headers), String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }
}
