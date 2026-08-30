package com.ee.lab.jta.exception;

public class QuotaExceededException extends Exception {
    public QuotaExceededException(String message) {
        super(message);
    }
}
