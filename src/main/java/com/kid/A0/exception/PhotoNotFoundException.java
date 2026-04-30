package com.kid.A0.exception;

public class PhotoNotFoundException extends RuntimeException {
    public PhotoNotFoundException(String message) {
        super(message);
    }
}
