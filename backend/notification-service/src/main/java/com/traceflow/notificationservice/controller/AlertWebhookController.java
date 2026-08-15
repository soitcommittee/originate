package com.traceflow.notificationservice.controller;

import com.traceflow.notificationservice.dto.AlertDeliveryResponse;
import com.traceflow.notificationservice.dto.AlertmanagerWebhook;
import com.traceflow.notificationservice.services.AlertDeliveryService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/alerts")
public class AlertWebhookController {
    private final AlertDeliveryService alertDeliveryService;

    public AlertWebhookController(AlertDeliveryService alertDeliveryService) {
        this.alertDeliveryService = alertDeliveryService;
    }

    @PostMapping("/alertmanager")
    public ResponseEntity<AlertDeliveryResponse> receive(@RequestBody AlertmanagerWebhook webhook) {
        return ResponseEntity.accepted().body(alertDeliveryService.deliver(webhook));
    }
}
