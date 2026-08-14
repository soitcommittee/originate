package com.traceflow.loanservice.exception;

import java.time.Instant;

public record ErrorResponse(
        Instant timestamp,
        int status,
        String errorCode,
        String message,
        String requestId
) {
}

