package com.petproject.integration.hub.processor;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.petproject.integration.mapper.OrderMapper;
import com.petproject.integration.model.Order;
import com.petproject.integration.xml.generated.CreateOrderRequest;
import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.Marshaller;
import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.springframework.stereotype.Component;

import java.io.StringWriter;

/**
 * Enterprise Integration Pattern "Message Translator": перетворює канонічне
 * JSON-повідомлення (REST/MQTT-світ) на XML-документ createOrderRequest,
 * що відповідає order.xsd і готовий бути вкладеним у SOAP-конверт для SOAP-каналу.
 * Саме такий переклад форматів — щоденна робота ESB/розробника інтеграцій.
 */
@Component
public class JsonToXmlOrderTranslator implements Processor {

    private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();

    @Override
    public void process(Exchange exchange) throws Exception {
        String json = exchange.getIn().getBody(String.class);
        Order order = objectMapper.readValue(json, Order.class);

        CreateOrderRequest request = new CreateOrderRequest();
        request.setOrder(OrderMapper.toXml(order));

        JAXBContext context = JAXBContext.newInstance(CreateOrderRequest.class);
        Marshaller marshaller = context.createMarshaller();
        marshaller.setProperty(Marshaller.JAXB_FRAGMENT, true); // без <?xml ... ?> — фрагмент для вкладення в soap:Body

        StringWriter writer = new StringWriter();
        marshaller.marshal(request, writer);

        exchange.getIn().setBody(writer.toString());
    }
}
