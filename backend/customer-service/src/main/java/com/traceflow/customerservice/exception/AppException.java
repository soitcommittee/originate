package com.traceflow.customerservice.exception;

import org.springframework.http.HttpStatus;

public class AppException extends RuntimeException {
    private final ErrorCode errorCode;
    private final HttpStatus status;

    public AppException(ErrorCode errorCode, String message, HttpStatus status) {
        super(message);
        this.errorCode = errorCode;
        this.status = status;
    }

    public ErrorCode getErrorCode() { return errorCode; }
    public HttpStatus getStatus() { return status; }
}

