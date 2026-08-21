package com.traceflow.notificationservice.services;

import com.traceflow.notificationservice.client.HaalandDebugSessionClient;
import com.traceflow.notificationservice.client.KubernetesPodLogClient;
import com.traceflow.notificationservice.dto.AlertDeliveryResponse;
import com.traceflow.notificationservice.dto.AlertmanagerWebhook;
import com.traceflow.notificationservice.dto.DebugSessionRequest;
import com.traceflow.notificationservice.exception.AlertDeliveryException;
import com.traceflow.notificationservice.exception.InvalidAlertException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
public class DebugSessionAlertService {
    private static final Logger log = LoggerFactory.getLogger(DebugSessionAlertService.class);

    private final KubernetesPodLogClient podLogClient;
    private final ErrorLogExtractor errorLogExtractor;
    private final HaalandDebugSessionClient haalandClient;
    private final String repoUrl;
    private final String baseRef;

    public DebugSessionAlertService(KubernetesPodLogClient podLogClient,
                                    ErrorLogExtractor errorLogExtractor,
                                    HaalandDebugSessionClient haalandClient,
                                    @Value("${haaland.repo-url:}") String repoUrl,
                                    @Value("${haaland.base-ref:main}") String baseRef) {
        this.podLogClient = podLogClient;
        this.errorLogExtractor = errorLogExtractor;
        this.haalandClient = haalandClient;
        this.repoUrl = repoUrl;
        this.baseRef = baseRef;
    }

    public AlertDeliveryResponse deliver(AlertmanagerWebhook webhook) {
        validate(webhook);
        String status = valueOrDefault(webhook.status(), "unknown").toUpperCase();
        String service = resolveService(webhook);
        if (!"FIRING".equals(status)) {
            log.info("debug_session_skipped targetService={} alertStatus={} reason=not-firing", service, status);
            return new AlertDeliveryResponse(false, service, status, webhook.alerts().size(), "haaland");
        }
        if (repoUrl.isBlank()) {
            throw new AlertDeliveryException("Haaland repository URL is not configured", null);
        }

        String excerpt = errorLogExtractor.extract(podLogClient.getRecentLogs(service));
        if (excerpt.isBlank()) {
            throw new AlertDeliveryException(
                    "No error excerpt found in recent logs for service " + service, null);
        }
        haalandClient.create(new DebugSessionRequest(repoUrl, service, excerpt,
                valueOrDefault(baseRef, "main")));
        log.info("debug_session_requested targetService={} alertStatus={} alertCount={} destination=haaland excerptChars={}",
                service, status, webhook.alerts().size(), excerpt.length());
        return new AlertDeliveryResponse(true, service, status, webhook.alerts().size(), "haaland");
    }

    private void validate(AlertmanagerWebhook webhook) {
        if (webhook == null || webhook.alerts() == null || webhook.alerts().isEmpty()) {
            throw new InvalidAlertException("Alertmanager payload must contain at least one alert");
        }
    }

    private String resolveService(AlertmanagerWebhook webhook) {
        String commonService = value(webhook.commonLabels(), "service");
        if (!commonService.isBlank()) return commonService;
        String firstService = value(webhook.alerts().get(0).labels(), "service");
        return firstService.isBlank() ? "platform" : firstService;
    }

    private String value(Map<String, String> values, String key) {
        if (values == null) return "";
        return valueOrDefault(values.get(key), "");
    }

    private String valueOrDefault(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }
}
