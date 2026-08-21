package com.traceflow.notificationservice.dto;

public record DebugSessionResponse(
        String sessionId,
        String documentPath,
        String pic,
        boolean larkDelivered
) {
}
