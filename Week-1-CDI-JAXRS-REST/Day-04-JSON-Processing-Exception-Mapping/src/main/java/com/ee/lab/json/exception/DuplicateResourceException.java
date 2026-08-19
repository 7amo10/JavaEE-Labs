package com.ee.lab.json.exception;

public class DuplicateResourceException extends RuntimeException {
    private final String existingId;

    public DuplicateResourceException(String existingId, String message) {
        super(message);
        this.existingId = existingId;
    }

    public String getExistingId() {
        return existingId;
    }
}
