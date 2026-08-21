package com.ee.lab.pulse.exception;

import java.util.Collections;
import java.util.Map;

public class InvalidSnapshotPayloadException extends RuntimeException {
    private final Map<String, String> fieldErrors;

    public InvalidSnapshotPayloadException(String message) {
        super(message);
        this.fieldErrors = Collections.emptyMap();
    }

    public InvalidSnapshotPayloadException(String message, Map<String, String> fieldErrors) {
        super(message);
        this.fieldErrors = fieldErrors;
    }

    public Map<String, String> getFieldErrors() { return fieldErrors; }
}
