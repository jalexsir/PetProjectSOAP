package com.petproject.integration.soap.config;

import org.springframework.boot.web.servlet.ServletRegistrationBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.ws.config.annotation.EnableWs;
import org.springframework.ws.config.annotation.WsConfigurerAdapter;
import org.springframework.ws.soap.server.endpoint.SoapFaultAnnotationExceptionResolver;
import org.springframework.ws.transport.http.MessageDispatcherServlet;
import org.springframework.ws.wsdl.wsdl11.DefaultWsdl11Definition;
import org.springframework.ws.wsdl.wsdl11.Wsdl11Definition;
import org.springframework.xml.xsd.SimpleXsdSchema;
import org.springframework.xml.xsd.XsdSchema;

/**
 * Contract-first налаштування: XSD (order.xsd, з common-model) є єдиним джерелом
 * істини, а WSDL для операцій getOrder/createOrder генерується автоматично
 * Spring-WS на його основі — саме так на практиці найчастіше будують SOAP-контракти
 * в enterprise-інтеграціях (не навпаки, "code-first").
 */
@EnableWs
@Configuration
public class WebServiceConfig extends WsConfigurerAdapter {

    private static final String NAMESPACE_URI = "http://petproject.integration/orders";

    @Bean
    public ServletRegistrationBean<MessageDispatcherServlet> messageDispatcherServlet(ApplicationContext applicationContext) {
        MessageDispatcherServlet servlet = new MessageDispatcherServlet();
        servlet.setApplicationContext(applicationContext);
        servlet.setTransformWsdlLocations(true);
        return new ServletRegistrationBean<>(servlet, "/ws/*");
    }

    // Доступний за адресою /ws/orders.wsdl (ім'я біна "orders" + суфікс .wsdl)
    @Bean(name = "orders")
    public Wsdl11Definition defaultWsdl11Definition(XsdSchema ordersSchema) {
        DefaultWsdl11Definition wsdl11Definition = new DefaultWsdl11Definition();
        wsdl11Definition.setPortTypeName("OrderPort");
        wsdl11Definition.setLocationUri("/ws");
        wsdl11Definition.setTargetNamespace(NAMESPACE_URI);
        wsdl11Definition.setSchema(ordersSchema);
        return wsdl11Definition;
    }

    @Bean
    public XsdSchema ordersSchema() {
        // order.xsd лежить на classpath у common-model (src/main/resources/xsd/order.xsd)
        return new SimpleXsdSchema(new ClassPathResource("xsd/order.xsd"));
    }

    // Перетворює @SoapFault-анотовані винятки (наприклад OrderNotFoundException) на справжній <soap:Fault>
    @Bean
    public SoapFaultAnnotationExceptionResolver exceptionResolver() {
        SoapFaultAnnotationExceptionResolver resolver = new SoapFaultAnnotationExceptionResolver();
        resolver.setOrder(1);
        return resolver;
    }
}
