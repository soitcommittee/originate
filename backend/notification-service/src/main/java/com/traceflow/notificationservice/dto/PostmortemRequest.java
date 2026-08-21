package com.traceflow.notificationservice.dto;

public record PostmortemRequest(
        String service,
        String errorCode,
        String endpoint,
        String requestId,
        String observedAt,
        String notes
) {
}
