package com.petproject.integration.hub.gateway;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Base64;

/** Реальний виклик REST-каналу (rest-service, /api/basic/orders — HTTP Basic). */
@Component
public class HttpRestChannelGateway implements RestChannelGateway {

    private static final Logger LOGGER = LoggerFactory.getLogger(HttpRestChannelGateway.class);

    private final HttpClient httpClient;
    private final String restServiceUrl;
    private final String basicAuthHeader;

    public HttpRestChannelGateway(
            @Value("${hub.rest-service.base-url:http://localhost:8082/api/basic/orders}") String restServiceUrl,
            @Value("${hub.rest-service.username:integration}") String username,
            @Value("${hub.rest-service.password:integration-pass}") String password) {
        this.httpClient = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build();
        this.restServiceUrl = restServiceUrl;
        this.basicAuthHeader = "Basic " + Base64.getEncoder()
                .encodeToString((username + ":" + password).getBytes(StandardCharsets.UTF_8));
    }

    @Override
    public void send(String orderJson) {
        HttpRequest request = HttpRequest.newBuilder(URI.create(restServiceUrl))
                .header("Content-Type", "application/json")
                .header("Authorization", basicAuthHeader)
                .timeout(Duration.ofSeconds(5))
                .POST(HttpRequest.BodyPublishers.ofString(orderJson, StandardCharsets.UTF_8))
                .build();
        try {
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            LOGGER.info("REST channel gateway -> {} : HTTP {}", restServiceUrl, response.statusCode());
        } catch (Exception e) {
            throw new IllegalStateException("Failed to forward order to REST channel: " + restServiceUrl, e);
        }
    }
}
