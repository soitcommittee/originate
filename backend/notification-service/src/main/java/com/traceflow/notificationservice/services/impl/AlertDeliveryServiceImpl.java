package com.traceflow.notificationservice.services.impl;

import com.traceflow.notificationservice.client.LarkWebhookClient;
import com.traceflow.notificationservice.config.LarkWebhookRegistry;
import com.traceflow.notificationservice.dto.AlertDeliveryResponse;
import com.traceflow.notificationservice.dto.AlertmanagerWebhook;
import com.traceflow.notificationservice.exception.AlertConfigurationException;
import com.traceflow.notificationservice.exception.InvalidAlertException;
import com.traceflow.notificationservice.services.AlertDeliveryService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
public class AlertDeliveryServiceImpl implements AlertDeliveryService {
    private static final Logger log = LoggerFactory.getLogger(AlertDeliveryServiceImpl.class);

    private final LarkWebhookRegistry webhookRegistry;
    private final LarkWebhookClient webhookClient;

    public AlertDeliveryServiceImpl(LarkWebhookRegistry webhookRegistry, LarkWebhookClient webhookClient) {
        this.webhookRegistry = webhookRegistry;
        this.webhookClient = webhookClient;
    }

    @Override
    public AlertDeliveryResponse deliver(AlertmanagerWebhook webhook) {
        if (webhook == null || webhook.alerts() == null || webhook.alerts().isEmpty()) {
            throw new InvalidAlertException("Alertmanager payload must contain at least one alert");
        }

        String service = resolveService(webhook);
        String destination = webhookRegistry.findFor(service)
                .orElseThrow(() -> new AlertConfigurationException(
                        "No Lark webhook configured for service " + service));
        String alertStatus = valueOrDefault(webhook.status(), "unknown").toUpperCase();
        String message = buildMessage(webhook, service, alertStatus);

        webhookClient.sendText(destination, message);
        log.info("lark_alert_delivered targetService={} alertStatus={} alertCount={} destination=lark-group",
                service, alertStatus, webhook.alerts().size());
        return new AlertDeliveryResponse(true, service, alertStatus, webhook.alerts().size(), "lark-group");
    }

    private String resolveService(AlertmanagerWebhook webhook) {
        String commonService = value(webhook.commonLabels(), "service");
        if (!commonService.isBlank()) return commonService;
        String firstService = value(webhook.alerts().get(0).labels(), "service");
        return firstService.isBlank() ? "platform" : firstService;
    }

    private String buildMessage(AlertmanagerWebhook webhook, String service, String status) {
        Map<String, String> labels = webhook.commonLabels() == null ? Map.of() : webhook.commonLabels();
        Map<String, String> annotations = webhook.commonAnnotations() == null ? Map.of() : webhook.commonAnnotations();
        AlertmanagerWebhook.Alert first = webhook.alerts().get(0);
        String alertName = valueOrDefault(labels.get("alertname"), value(first.labels(), "alertname"));
        String severity = valueOrDefault(labels.get("severity"), value(first.labels(), "severity"));
        String summary = valueOrDefault(annotations.get("summary"), value(first.annotations(), "summary"));
        String description = valueOrDefault(annotations.get("description"), value(first.annotations(), "description"));

        return "[Originate " + status + "] " + valueOrDefault(alertName, "Monitoring alert") + "\n"
                + "Service: " + service + "\n"
                + "Severity: " + valueOrDefault(severity, "warning") + "\n"
                + "Alert count: " + webhook.alerts().size() + "\n"
                + "Summary: " + valueOrDefault(summary, "No summary provided") + "\n"
                + "Description: " + valueOrDefault(description, "No description provided") + "\n"
                + "Started at: " + valueOrDefault(first.startsAt(), "unknown") + "\n"
                + "Prometheus: " + valueOrDefault(first.generatorURL(), "not available");
    }

    private String value(Map<String, String> values, String key) {
        if (values == null) return "";
        return valueOrDefault(values.get(key), "");
    }

    private String valueOrDefault(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }
}
