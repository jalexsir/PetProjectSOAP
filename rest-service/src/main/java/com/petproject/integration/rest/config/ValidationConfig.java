package com.petproject.integration.rest.config;

import com.petproject.integration.validation.JsonSchemaValidator;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ValidationConfig {

    @Bean
    public JsonSchemaValidator orderJsonSchemaValidator() {
        // order-schema.json постачається common-model (src/main/resources/json-schema/order-schema.json)
        return new JsonSchemaValidator("json-schema/order-schema.json");
    }
}
