package com.petproject.integration.hub.processor;

import com.petproject.integration.hub.exception.CrossDomainViolationException;
import com.petproject.integration.validation.JsonSchemaValidator;
import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Спрощена, навчальна модель Cross Domain Solution (CDS): "шлюз довіри" на межі
 * між зовнішнім (недовіреним) доменом і внутрішнім (довіреним) інтеграційним
 * ландшафтом. Справжній CDS (наприклад Owl, Fox DataDiode) працює на мережевому
 * рівні, часто навіть з фізичною однонаправленістю (data diode); тут та сама ідея
 * відтворена на рівні повідомлення:
 *   1) контрактна валідація (whitelist полів/типів через JSON Schema — усе,
 *      чого немає в контракті, автоматично відкидається: additionalProperties=false),
 *   2) аудит-лог кожного рішення ALLOW/DENY (обов'язкова вимога до CDS —
 *      трасованість перетину межі домену),
 *   3) відмова "за замовчуванням" (fail-closed): будь-яке порушення контракту
 *      зупиняє повідомлення ДО того, як воно потрапить у внутрішній домен.
 */
@Component
public class CrossDomainGuardProcessor implements Processor {

    private static final Logger AUDIT = LoggerFactory.getLogger("CDS_AUDIT");

    private final JsonSchemaValidator orderJsonSchemaValidator;

    public CrossDomainGuardProcessor(JsonSchemaValidator orderJsonSchemaValidator) {
        this.orderJsonSchemaValidator = orderJsonSchemaValidator;
    }

    @Override
    public void process(Exchange exchange) {
        String body = exchange.getIn().getBody(String.class);
        String channel = exchange.getIn().getHeader("channel", String.class);

        List<String> violations = orderJsonSchemaValidator.validate(body);
        if (!violations.isEmpty()) {
            AUDIT.warn("CDS_DENY channel={} reasons={}", channel, violations);
            throw new CrossDomainViolationException(violations);
        }

        AUDIT.info("CDS_ALLOW channel={} payloadBytes={}", channel, body.getBytes().length);
    }
}
