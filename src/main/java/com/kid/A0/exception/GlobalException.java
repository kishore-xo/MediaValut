package com.kid.A0.exception;

import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;

@Slf4j
@RestControllerAdvice
public class GlobalException {

    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<ApiError> runtimeException(RuntimeException e, HttpServletRequest request) {
        HttpStatus status = HttpStatus.BAD_REQUEST;

        ApiError apiError = new ApiError(
                status.value(),
                e.getMessage(),
                request.getRequestURI(),
                status.getReasonPhrase()
        );
        return ResponseEntity
                .status(status)
                .contentType(MediaType.APPLICATION_JSON)
                .body(apiError);
    }

    @ExceptionHandler(RateLimitException.class)
    public ResponseEntity<ApiError> rateLimitException(RateLimitException e, HttpServletRequest request) {
        HttpStatus status = HttpStatus.TOO_MANY_REQUESTS;
        ApiError apiError = new ApiError(
                status.value(),
                e.getMessage(),
                request.getRequestURI(),
                status.getReasonPhrase()
        );
        return ResponseEntity.status(status)
                .contentType(MediaType.APPLICATION_JSON)
                .body(apiError);
    }

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<ApiError> responseStatusException(ResponseStatusException e, HttpServletRequest request) {

        HttpStatus status = (HttpStatus) e.getStatusCode();
        ApiError apiError = new ApiError(
                status.value(),
                e.getReason(),
                request.getRequestURI(),
                status.getReasonPhrase()
        );
        return ResponseEntity.status(status)
                .body(apiError);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiError> exception(Exception e, HttpServletRequest request) {
        HttpStatus status = HttpStatus.INTERNAL_SERVER_ERROR;
        ApiError apiError = new ApiError(
                status.value(),
                e.getMessage(),
                request.getRequestURI(),
                status.getReasonPhrase()
        );
        return ResponseEntity.status(status)
                .contentType(MediaType.APPLICATION_JSON)
                .body(apiError);
    }
}
