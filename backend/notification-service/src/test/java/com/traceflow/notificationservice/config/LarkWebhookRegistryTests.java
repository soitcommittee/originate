package com.traceflow.notificationservice.config;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class LarkWebhookRegistryTests {
    @Test
    void selectsServiceWebhookAndFallsBackToPlatform() {
        var registry = new LarkWebhookRegistry(
                "customer", "loan", "", "decision", "notification", "frontend", "platform");

        assertThat(registry.findFor("LOAN-SERVICE")).contains("loan");
        assertThat(registry.findFor("product-service")).contains("platform");
        assertThat(registry.findFor("unknown-service")).contains("platform");
    }

    @Test
    void returnsEmptyWhenNoMatchingOrPlatformWebhookExists() {
        var registry = new LarkWebhookRegistry("", "", "", "", "", "", "");

        assertThat(registry.findFor("loan-service")).isEmpty();
    }
}
