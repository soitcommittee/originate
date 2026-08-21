package com.traceflow.notificationservice.services.impl;

import com.traceflow.notificationservice.client.LarkWebhookClient;
import com.traceflow.notificationservice.config.LarkWebhookRegistry;
import com.traceflow.notificationservice.dto.AlertmanagerWebhook;
import com.traceflow.notificationservice.exception.AlertConfigurationException;
import com.traceflow.notificationservice.exception.InvalidAlertException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AlertDeliveryServiceImplTests {
    private LarkWebhookRegistry registry;
    private LarkWebhookClient client;
    private AlertDeliveryServiceImpl service;

    @BeforeEach
    void setUp() {
        registry = mock(LarkWebhookRegistry.class);
        client = mock(LarkWebhookClient.class);
        service = new AlertDeliveryServiceImpl(registry, client);
    }

    @Test
    void routesAlertToServiceGroup() {
        when(registry.findFor("loan-service")).thenReturn(Optional.of("https://example.invalid/hook"));

        var response = service.deliver(payload("firing", Map.of("service", "loan-service"), Map.of()));

        assertThat(response.delivered()).isTrue();
        assertThat(response.service()).isEqualTo("loan-service");
        assertThat(response.alertStatus()).isEqualTo("FIRING");
        verify(client).sendText(contains("example.invalid"), contains("Service: loan-service"));
        verify(client).sendText(contains("example.invalid"), contains("Started at: 16 Aug 2026, 8:00:00 AM MYT"));
    }

    @Test
    void resolvesServiceFromFirstAlertWhenCommonLabelIsMissing() {
        when(registry.findFor("decision-service")).thenReturn(Optional.of("https://example.invalid/hook"));

        var response = service.deliver(payload("resolved", Map.of(), Map.of("service", "decision-service")));

        assertThat(response.service()).isEqualTo("decision-service");
        assertThat(response.alertStatus()).isEqualTo("RESOLVED");
        verify(client).sendText(contains("example.invalid"), contains("Originate RESOLVED"));
    }

    @Test
    void usesPlatformRouteWhenNoServiceLabelExists() {
        when(registry.findFor("platform")).thenReturn(Optional.of("https://example.invalid/platform"));

        var response = service.deliver(payload(null, Map.of(), Map.of()));

        assertThat(response.service()).isEqualTo("platform");
        assertThat(response.alertStatus()).isEqualTo("UNKNOWN");
    }

    @Test
    void rejectsPayloadWithoutAlerts() {
        var payload = new AlertmanagerWebhook("firing", Map.of(), Map.of(), List.of());

        assertThatThrownBy(() -> service.deliver(payload)).isInstanceOf(InvalidAlertException.class);
        assertThatThrownBy(() -> service.deliver(null)).isInstanceOf(InvalidAlertException.class);
    }

    @Test
    void failsWhenNoWebhookIsConfigured() {
        when(registry.findFor("product-service")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.deliver(payload("firing", Map.of("service", "product-service"), Map.of())))
                .isInstanceOf(AlertConfigurationException.class)
                .hasMessageContaining("product-service");
    }

    private AlertmanagerWebhook payload(String status, Map<String, String> commonLabels,
                                        Map<String, String> alertLabels) {
        Map<String, String> labels = new java.util.HashMap<>(alertLabels);
        labels.putIfAbsent("alertname", "Excessive5xxErrors");
        labels.putIfAbsent("severity", "critical");
        var alert = new AlertmanagerWebhook.Alert("firing", labels,
                Map.of("summary", "High error volume", "description", "One error in five minutes"),
                "2026-08-16T00:00:00Z", null, "http://prometheus/graph", "fingerprint");
        return new AlertmanagerWebhook(status, commonLabels,
                Map.of("summary", "High error volume", "description", "One error in five minutes"),
                List.of(alert));
    }
}
