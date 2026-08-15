package com.traceflow.notificationservice.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Locale;
import java.util.Map;
import java.util.Optional;

@Component
public class LarkWebhookRegistry {
    private final Map<String, String> webhooks;

    public LarkWebhookRegistry(
            @Value("${lark.webhooks.customer-service:}") String customerService,
            @Value("${lark.webhooks.loan-service:}") String loanService,
            @Value("${lark.webhooks.product-service:}") String productService,
            @Value("${lark.webhooks.decision-service:}") String decisionService,
            @Value("${lark.webhooks.notification-service:}") String notificationService,
            @Value("${lark.webhooks.frontend:}") String frontend,
            @Value("${lark.webhooks.platform:}") String platform) {
        this.webhooks = Map.of(
                "customer-service", customerService,
                "loan-service", loanService,
                "product-service", productService,
                "decision-service", decisionService,
                "notification-service", notificationService,
                "frontend", frontend,
                "platform", platform
        );
    }

    public Optional<String> findFor(String service) {
        String normalized = service == null ? "platform" : service.toLowerCase(Locale.ROOT).trim();
        String configured = webhooks.getOrDefault(normalized, "");
        if (configured.isBlank()) configured = webhooks.getOrDefault("platform", "");
        return configured.isBlank() ? Optional.empty() : Optional.of(configured);
    }
}
