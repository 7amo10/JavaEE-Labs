package com.ee.lab.json.exception;

public class ResourceNotFoundException extends RuntimeException {
    private final String resourceId;

    public ResourceNotFoundException(String resourceId, String message) {
        super(message);
        this.resourceId = resourceId;
    }

    public String getResourceId() {
        return resourceId;
    }
}
