package com.kid.A0.exception;

import com.fasterxml.jackson.annotation.JsonFormat;

import java.time.LocalDateTime;

public record ApiError(int status, String message, String path, String error,
                       @JsonFormat(pattern = "yyyy-MM-dd HH-mm-ss") LocalDateTime time) {

    public ApiError(int status, String message, String path, String error) {
        this(status, message, path, error, LocalDateTime.now());
    }
}
