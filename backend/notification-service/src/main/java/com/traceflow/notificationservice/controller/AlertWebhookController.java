package com.traceflow.notificationservice.controller;

import com.traceflow.notificationservice.dto.AlertDeliveryResponse;
import com.traceflow.notificationservice.dto.AlertmanagerWebhook;
import com.traceflow.notificationservice.services.AlertDeliveryService;
import com.traceflow.notificationservice.services.DebugSessionAlertService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/alerts")
public class AlertWebhookController {
    private final AlertDeliveryService alertDeliveryService;
    private final DebugSessionAlertService debugSessionAlertService;

    public AlertWebhookController(AlertDeliveryService alertDeliveryService,
                                  DebugSessionAlertService debugSessionAlertService) {
        this.alertDeliveryService = alertDeliveryService;
        this.debugSessionAlertService = debugSessionAlertService;
    }

    @PostMapping("/alertmanager")
    public ResponseEntity<AlertDeliveryResponse> receive(@RequestBody AlertmanagerWebhook webhook) {
        return ResponseEntity.accepted().body(alertDeliveryService.deliver(webhook));
    }

    @PostMapping("/haaland")
    public ResponseEntity<AlertDeliveryResponse> receiveForDebugSession(@RequestBody AlertmanagerWebhook webhook) {
        return ResponseEntity.accepted().body(debugSessionAlertService.deliver(webhook));
    }
}
