package com.traceflow.productservice.exception;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.Instant;

@RestControllerAdvice
public class GlobalExceptionHandler {
    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(AppException.class)
    public ResponseEntity<ErrorResponse> handleAppException(AppException ex) {
        log.warn("request_failed errorCode={} status={} message={}",
                ex.getErrorCode(), ex.getStatus().value(), ex.getMessage());
        return response(ex.getStatus(), ex.getErrorCode().name(), ex.getMessage());
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleUnexpected(Exception ex) {
        log.error("unexpected_product_service_error", ex);
        return response(HttpStatus.INTERNAL_SERVER_ERROR, ErrorCode.INTERNAL_ERROR.name(),
                "Unexpected service error");
    }

    private ResponseEntity<ErrorResponse> response(HttpStatus status, String code, String message) {
        return ResponseEntity.status(status).body(new ErrorResponse(
                Instant.now(), status.value(), code, message, MDC.get("requestId")));
    }
}
