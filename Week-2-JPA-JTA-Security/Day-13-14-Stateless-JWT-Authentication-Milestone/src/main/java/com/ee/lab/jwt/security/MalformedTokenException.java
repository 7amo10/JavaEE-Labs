package com.ee.lab.jwt.security;

public class MalformedTokenException extends Exception {
    public MalformedTokenException(String message) {
        super(message);
    }

    public MalformedTokenException(String message, Throwable cause) {
        super(message, cause);
    }
}
