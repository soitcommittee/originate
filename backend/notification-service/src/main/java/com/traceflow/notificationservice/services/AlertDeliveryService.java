package com.traceflow.notificationservice.services;

import com.traceflow.notificationservice.dto.AlertDeliveryResponse;
import com.traceflow.notificationservice.dto.AlertmanagerWebhook;

public interface AlertDeliveryService {
    AlertDeliveryResponse deliver(AlertmanagerWebhook webhook);
}
