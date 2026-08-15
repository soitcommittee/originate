package com.traceflow.notificationservice.dto;

public record AlertDeliveryResponse(
        boolean delivered,
        String service,
        String alertStatus,
        int alertCount,
        String destination
) {
}
