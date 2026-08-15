package com.traceflow.notificationservice.exception;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {
    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(InvalidAlertException.class)
    public ResponseEntity<Map<String, Object>> invalidAlert(InvalidAlertException ex) {
        log.warn("alert_webhook_rejected reason={}", ex.getMessage());
        return response(HttpStatus.BAD_REQUEST, "INVALID_ALERT_PAYLOAD", ex.getMessage());
    }

    @ExceptionHandler(AlertConfigurationException.class)
    public ResponseEntity<Map<String, Object>> missingConfiguration(AlertConfigurationException ex) {
        log.error("alert_delivery_configuration_error message={}", ex.getMessage());
        return response(HttpStatus.SERVICE_UNAVAILABLE, "LARK_WEBHOOK_NOT_CONFIGURED", ex.getMessage());
    }

    @ExceptionHandler(AlertDeliveryException.class)
    public ResponseEntity<Map<String, Object>> deliveryFailure(AlertDeliveryException ex) {
        log.error("alert_delivery_failed message={}", ex.getMessage(), ex);
        return response(HttpStatus.BAD_GATEWAY, "LARK_DELIVERY_FAILED", ex.getMessage());
    }

    private ResponseEntity<Map<String, Object>> response(HttpStatus status, String code, String message) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("timestamp", Instant.now());
        body.put("status", status.value());
        body.put("errorCode", code);
        body.put("message", message);
        body.put("requestId", MDC.get("requestId"));
        return ResponseEntity.status(status).body(body);
    }
}
