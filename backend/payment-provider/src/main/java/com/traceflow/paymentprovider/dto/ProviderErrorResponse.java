package com.traceflow.paymentprovider.dto;

import java.time.Instant;

public record ProviderErrorResponse(Instant timestamp, int status, String code, String message, String requiredField) {
}
