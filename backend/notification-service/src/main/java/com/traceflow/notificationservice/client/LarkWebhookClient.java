package com.traceflow.notificationservice.client;

import com.traceflow.notificationservice.exception.AlertDeliveryException;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.util.Map;

@Component
public class LarkWebhookClient {
    private final RestClient restClient;

    public LarkWebhookClient(RestClient.Builder builder) {
        this.restClient = builder.build();
    }

    public void sendText(String webhookUrl, String text) {
        try {
            Map<?, ?> response = restClient.post()
                    .uri(webhookUrl)
                    .body(Map.of(
                            "msg_type", "text",
                            "content", Map.of("text", text)
                    ))
                    .retrieve()
                    .body(Map.class);
            if (!accepted(response)) {
                throw new AlertDeliveryException("Lark webhook returned an unsuccessful response", null);
            }
        } catch (RestClientException ex) {
            throw new AlertDeliveryException("Lark webhook rejected the alert notification", ex);
        }
    }

    private boolean accepted(Map<?, ?> response) {
        if (response == null || response.isEmpty()) return false;
        Object code = response.containsKey("code") ? response.get("code") : response.get("StatusCode");
        if (code == null) return false;
        if (code instanceof Number number) return number.intValue() == 0;
        return "0".equals(code.toString());
    }
}
