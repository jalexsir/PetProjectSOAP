package com.petproject.integration.mapper;

import com.petproject.integration.model.Order;
import com.petproject.integration.model.OrderItem;
import com.petproject.integration.model.OrderStatus;
import com.petproject.integration.xml.generated.ObjectFactory;
import com.petproject.integration.xml.generated.OrderItemXml;
import com.petproject.integration.xml.generated.OrderItems;
import com.petproject.integration.xml.generated.OrderXml;

import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.DatatypeFactory;
import javax.xml.datatype.XMLGregorianCalendar;
import java.math.BigInteger;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.stream.Collectors;

/**
 * "Message Translator" (Enterprise Integration Pattern): переклад між XML-світом
 * (JAXB-класи, згенеровані xjc з order.xsd — використовуються SOAP-каналом)
 * та JSON-світом (POJO з пакету model — використовуються REST-каналом).
 * Саме цей клас робить integration-hub здатним "перекладати" повідомлення
 * між протоколами, як це робить ESB.
 */
public final class OrderMapper {

    private static final ObjectFactory XML_FACTORY = new ObjectFactory();
    private static final DatatypeFactory DATATYPE_FACTORY = createDatatypeFactory();

    private OrderMapper() {
    }

    public static Order toPojo(OrderXml xmlOrder) {
        if (xmlOrder == null) {
            return null;
        }
        Order order = new Order();
        order.setOrderId(xmlOrder.getOrderId());
        order.setCustomerId(xmlOrder.getCustomerId());
        order.setStatus(OrderStatus.valueOf(xmlOrder.getStatus().value()));
        order.setCreatedAt(toOffsetDateTime(xmlOrder.getCreatedAt()));

        List<OrderItem> items = xmlOrder.getItems().getItem().stream()
                .map(i -> new OrderItem(i.getSku(), i.getQuantity().intValueExact(), i.getUnitPrice()))
                .collect(Collectors.toList());
        order.setItems(items);
        return order;
    }

    public static OrderXml toXml(Order order) {
        if (order == null) {
            return null;
        }
        OrderXml xmlOrder = XML_FACTORY.createOrderXml();
        xmlOrder.setOrderId(order.getOrderId());
        xmlOrder.setCustomerId(order.getCustomerId());
        // com.petproject.integration.xml.generated.OrderStatus лишається з повним шляхом:
        // model.OrderStatus вище вже займає коротке ім'я "OrderStatus" в цьому файлі,
        // а сам XJC-enum перейменувати не вдалось (див. коментар у src/main/xjb/bindings.xjb).
        xmlOrder.setStatus(com.petproject.integration.xml.generated.OrderStatus.fromValue(order.getStatus().name()));
        xmlOrder.setCreatedAt(toXmlGregorianCalendar(order.getCreatedAt()));

        OrderItems xmlItems = XML_FACTORY.createOrderItems();
        for (OrderItem item : order.getItems()) {
            OrderItemXml xmlItem = XML_FACTORY.createOrderItemXml();
            xmlItem.setSku(item.getSku());
            xmlItem.setQuantity(BigInteger.valueOf(item.getQuantity()));
            xmlItem.setUnitPrice(item.getUnitPrice());
            xmlItems.getItem().add(xmlItem);
        }
        xmlOrder.setItems(xmlItems);
        return xmlOrder;
    }

    private static OffsetDateTime toOffsetDateTime(XMLGregorianCalendar calendar) {
        if (calendar == null) {
            return null;
        }
        return calendar.toGregorianCalendar().toZonedDateTime().toOffsetDateTime();
    }

    private static XMLGregorianCalendar toXmlGregorianCalendar(OffsetDateTime dateTime) {
        if (dateTime == null) {
            return null;
        }
        GregorianCalendar gregorianCalendar = GregorianCalendar.from(dateTime.atZoneSameInstant(ZoneOffset.UTC));
        return DATATYPE_FACTORY.newXMLGregorianCalendar(gregorianCalendar);
    }

    private static DatatypeFactory createDatatypeFactory() {
        try {
            return DatatypeFactory.newInstance();
        } catch (DatatypeConfigurationException e) {
            throw new IllegalStateException("Unable to initialize DatatypeFactory for XML <-> JSON date mapping", e);
        }
    }
}
