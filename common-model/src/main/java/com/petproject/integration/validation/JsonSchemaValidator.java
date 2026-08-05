package com.petproject.integration.validation;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.networknt.schema.JsonSchema;
import com.networknt.schema.JsonSchemaFactory;
import com.networknt.schema.SpecVersion;
import com.networknt.schema.ValidationMessage;

import java.io.InputStream;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Тонка обгортка над networknt/json-schema-validator.
 * Демонструє те саме, що XSD робить для SOAP: контрактну валідацію вхідного
 * повідомлення (JSON) відносно опублікованої схеми (order-schema.json)
 * ДО того, як повідомлення потрапляє в бізнес-логіку.
 */
public class JsonSchemaValidator {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private final JsonSchema schema;

    public JsonSchemaValidator(String classpathSchemaLocation) {
        JsonSchemaFactory factory = JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V7);
        try (InputStream schemaStream = getClass().getClassLoader().getResourceAsStream(classpathSchemaLocation)) {
            if (schemaStream == null) {
                throw new IllegalArgumentException("JSON schema not found on classpath: " + classpathSchemaLocation);
            }
            this.schema = factory.getSchema(schemaStream);
        } catch (Exception e) {
            throw new IllegalStateException("Unable to load JSON schema: " + classpathSchemaLocation, e);
        }
    }

    /** Повертає список повідомлень про порушення схеми; порожній список = валідний JSON. */
    public List<String> validate(String json) {
        try {
            JsonNode node = OBJECT_MAPPER.readTree(json);
            return validate(node);
        } catch (Exception e) {
            return List.of("Malformed JSON: " + e.getMessage());
        }
    }

    public List<String> validate(JsonNode node) {
        Set<ValidationMessage> messages = schema.validate(node);
        return messages.stream().map(ValidationMessage::getMessage).collect(Collectors.toList());
    }
}
