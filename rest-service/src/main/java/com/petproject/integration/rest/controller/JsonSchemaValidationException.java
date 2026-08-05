package com.petproject.integration.rest.controller;

import java.util.List;

public class JsonSchemaValidationException extends RuntimeException {

    private final List<String> errors;

    public JsonSchemaValidationException(List<String> errors) {
        super("JSON schema validation failed: " + errors);
        this.errors = errors;
    }

    public List<String> getErrors() {
        return errors;
    }
}
