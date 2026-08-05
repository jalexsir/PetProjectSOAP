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

/**
 * Реальний виклик SOAP-каналу (soap-service): загортає перекладений XML
 * (createOrderRequest) у soap:Envelope і надсилає POST на /ws із заголовком
 * Authorization: Basic — той самий механізм автентифікації, що й на прямому
 * SOAP-каналі (SoapSecurityConfig у soap-service).
 */
@Component
public class HttpSoapChannelGateway implements SoapChannelGateway {

    private static final Logger LOGGER = LoggerFactory.getLogger(HttpSoapChannelGateway.class);

    private final HttpClient httpClient;
    private final String soapServiceUrl;
    private final String basicAuthHeader;

    public HttpSoapChannelGateway(
            @Value("${hub.soap-service.base-url:http://localhost:8081/ws}") String soapServiceUrl,
            @Value("${hub.soap-service.username:integration}") String username,
            @Value("${hub.soap-service.password:integration-pass}") String password) {
        this.httpClient = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build();
        this.soapServiceUrl = soapServiceUrl;
        this.basicAuthHeader = "Basic " + Base64.getEncoder()
                .encodeToString((username + ":" + password).getBytes(StandardCharsets.UTF_8));
    }

    @Override
    public void send(String createOrderRequestXmlFragment) {
        String envelope = "<soap:Envelope xmlns:soap=\"http://schemas.xmlsoap.org/soap/envelope/\">"
                + "<soap:Body>" + createOrderRequestXmlFragment + "</soap:Body></soap:Envelope>";

        HttpRequest request = HttpRequest.newBuilder(URI.create(soapServiceUrl))
                .header("Content-Type", "text/xml; charset=UTF-8")
                .header("Authorization", basicAuthHeader)
                .timeout(Duration.ofSeconds(5))
                .POST(HttpRequest.BodyPublishers.ofString(envelope, StandardCharsets.UTF_8))
                .build();
        try {
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            LOGGER.info("SOAP channel gateway -> {} : HTTP {}", soapServiceUrl, response.statusCode());
        } catch (Exception e) {
            throw new IllegalStateException("Failed to forward order to SOAP channel: " + soapServiceUrl, e);
        }
    }
}
