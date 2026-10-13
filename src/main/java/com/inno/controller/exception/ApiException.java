package com.inno.controller.exception;

import org.springframework.http.HttpStatus;

import java.time.LocalDateTime;

public record ApiException(String errorCode, String errorMessage,
                           HttpStatus httpStatus, LocalDateTime timestamp) {

    public String getErrorMessage() {
        return errorMessage;
    }

    public HttpStatus getHttpStatus() {
        return httpStatus;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public String getErrorCode() {
        return errorCode;
    }
}
