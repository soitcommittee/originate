package com.traceflow.loanservice.exception;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.Instant;

@RestControllerAdvice
public class GlobalExceptionHandler {
    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(PaymentGatewayTimeoutException.class)
    public ResponseEntity<ErrorResponse> handlePaymentTimeout(PaymentGatewayTimeoutException ex) {
        log.error("production_incident component=payment-gateway operation=disburse errorCode={} message={}",
                ErrorCode.DISBURSEMENT_GATEWAY_TIMEOUT, ex.getMessage(), ex);
        return response(HttpStatus.BAD_GATEWAY, ErrorCode.DISBURSEMENT_GATEWAY_TIMEOUT.name(),
                "Disbursement is temporarily unavailable. Retry after the payment gateway recovers.");
    }

    @ExceptionHandler(ThirdPartyApiContractException.class)
    public ResponseEntity<ErrorResponse> handleThirdPartyContractMismatch(ThirdPartyApiContractException ex) {
        log.error("production_incident component=third-party-disbursement operation=disburse errorCode={} message={}",
                ErrorCode.THIRD_PARTY_API_CONTRACT_MISMATCH, ex.getMessage(), ex);
        return response(HttpStatus.BAD_GATEWAY, ErrorCode.THIRD_PARTY_API_CONTRACT_MISMATCH.name(),
                "Disbursement provider API changed and rejected the legacy request payload.");
    }

    @ExceptionHandler(AppException.class)
    public ResponseEntity<ErrorResponse> handleAppException(AppException ex) {
        if (ex.getStatus().is5xxServerError()) {
            log.error("request_failed errorCode={} status={} message={}", ex.getErrorCode(), ex.getStatus().value(), ex.getMessage(), ex);
        } else {
            log.warn("request_failed errorCode={} status={} message={}", ex.getErrorCode(), ex.getStatus().value(), ex.getMessage());
        }
        return response(ex.getStatus(), ex.getErrorCode().name(), ex.getMessage());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidation(MethodArgumentNotValidException ex) {
        String message = ex.getBindingResult().getFieldErrors().stream()
                .findFirst()
                .map(error -> error.getField() + " " + error.getDefaultMessage())
                .orElse("Request validation failed");
        log.warn("request_validation_failed message={}", message);
        return response(HttpStatus.BAD_REQUEST, ErrorCode.VALIDATION_ERROR.name(), message);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleUnexpected(Exception ex) {
        log.error("unexpected_loan_service_error", ex);
        return response(HttpStatus.INTERNAL_SERVER_ERROR, ErrorCode.INTERNAL_ERROR.name(), "Unexpected service error");
    }

    private ResponseEntity<ErrorResponse> response(HttpStatus status, String code, String message) {
        return ResponseEntity.status(status).body(new ErrorResponse(
                Instant.now(), status.value(), code, message, MDC.get("requestId")));
    }
}
