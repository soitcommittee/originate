package com.traceflow.customerservice.exception;

import java.time.Instant;

public record ErrorResponse(
        Instant timestamp,
        int status,
        String errorCode,
        String message,
        String requestId
) {
}

