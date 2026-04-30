package com.kid.A0.exception;

public class RateLimitException extends RuntimeException {
    public RateLimitException(String message) {
        super(message);
    }
}
