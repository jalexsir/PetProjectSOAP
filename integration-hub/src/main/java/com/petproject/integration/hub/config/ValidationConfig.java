package com.petproject.integration.hub.config;

import com.petproject.integration.validation.JsonSchemaValidator;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ValidationConfig {

    @Bean
    public JsonSchemaValidator orderJsonSchemaValidator() {
        return new JsonSchemaValidator("json-schema/order-schema.json");
    }
}
