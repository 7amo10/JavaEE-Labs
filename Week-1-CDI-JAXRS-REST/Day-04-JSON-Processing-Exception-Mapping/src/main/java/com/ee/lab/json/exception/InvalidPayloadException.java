package com.ee.lab.json.exception;

import java.util.Collections;
import java.util.Map;

public class InvalidPayloadException extends RuntimeException {
    private final Map<String, String> fieldErrors;

    public InvalidPayloadException(String message) {
        super(message);
        this.fieldErrors = Collections.emptyMap();
    }

    public InvalidPayloadException(String message, Map<String, String> fieldErrors) {
        super(message);
        this.fieldErrors = fieldErrors;
    }

    public Map<String, String> getFieldErrors() {
        return fieldErrors;
    }
}
