package com.traceflow.notificationservice.dto;

import java.util.List;
import java.util.Map;

public record AlertmanagerWebhook(
        String status,
        Map<String, String> commonLabels,
        Map<String, String> commonAnnotations,
        List<Alert> alerts
) {
    public record Alert(
            String status,
            Map<String, String> labels,
            Map<String, String> annotations,
            String startsAt,
            String endsAt,
            String generatorURL,
            String fingerprint
    ) {
    }
}
