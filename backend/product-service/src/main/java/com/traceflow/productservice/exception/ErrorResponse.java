package com.traceflow.productservice.exception;

import java.time.Instant;

public record ErrorResponse(Instant timestamp, int status, String code, String message, String requestId) {
}
