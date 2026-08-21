package com.traceflow.notificationservice.services;

import com.traceflow.notificationservice.client.HaalandDebugSessionClient;
import com.traceflow.notificationservice.client.KubernetesPodLogClient;
import com.traceflow.notificationservice.dto.AlertmanagerWebhook;
import com.traceflow.notificationservice.dto.DebugSessionRequest;
import com.traceflow.notificationservice.exception.AlertDeliveryException;
import com.traceflow.notificationservice.exception.InvalidAlertException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class DebugSessionAlertServiceTests {
    private KubernetesPodLogClient podLogClient;
    private HaalandDebugSessionClient haalandClient;
    private DebugSessionAlertService service;

    @BeforeEach
    void setUp() {
        podLogClient = mock(KubernetesPodLogClient.class);
        haalandClient = mock(HaalandDebugSessionClient.class);
        service = new DebugSessionAlertService(podLogClient, new ErrorLogExtractor(), haalandClient,
                "https://github.com/chiewhui1113/originate.git", "main");
    }

    @Test
    void createsDebugSessionFromErrorExcerpt() {
        when(podLogClient.getRecentLogs("loan-service")).thenReturn("""
                level=INFO message=ready
                level=ERROR message=failed java.lang.IllegalStateException: boom
                    at com.traceflow.LoanService.run(LoanService.java:1)
                level=INFO message=done
                """);

        var response = service.deliver(payload("firing"));

        assertThat(response.delivered()).isTrue();
        assertThat(response.destination()).isEqualTo("haaland");
        ArgumentCaptor<DebugSessionRequest> request = ArgumentCaptor.forClass(DebugSessionRequest.class);
        verify(haalandClient).create(request.capture());
        assertThat(request.getValue().serviceName()).isEqualTo("loan-service");
        assertThat(request.getValue().repoUrl()).endsWith("originate.git");
        assertThat(request.getValue().logText()).contains("IllegalStateException").doesNotContain("message=ready");
    }

    @Test
    void skipsResolvedAlertWithoutReadingLogs() {
        var response = service.deliver(payload("resolved"));

        assertThat(response.delivered()).isFalse();
        verify(podLogClient, never()).getRecentLogs("loan-service");
        verify(haalandClient, never()).create(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void failsWhenRecentLogsContainNoError() {
        when(podLogClient.getRecentLogs("loan-service")).thenReturn("level=INFO message=healthy");

        assertThatThrownBy(() -> service.deliver(payload("firing")))
                .isInstanceOf(AlertDeliveryException.class)
                .hasMessageContaining("No error excerpt");
        verify(haalandClient, never()).create(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void rejectsMissingAlerts() {
        assertThatThrownBy(() -> service.deliver(null)).isInstanceOf(InvalidAlertException.class);
        assertThatThrownBy(() -> service.deliver(new AlertmanagerWebhook("firing", Map.of(), Map.of(), List.of())))
                .isInstanceOf(InvalidAlertException.class);
    }

    private AlertmanagerWebhook payload(String status) {
        var alert = new AlertmanagerWebhook.Alert(status,
                Map.of("service", "loan-service", "alertname", "Excessive5xxErrors"),
                Map.of("summary", "errors"), "2026-08-21T00:00:00Z", null, "manual", "fp");
        return new AlertmanagerWebhook(status, Map.of("service", "loan-service"), Map.of(), List.of(alert));
    }
}
