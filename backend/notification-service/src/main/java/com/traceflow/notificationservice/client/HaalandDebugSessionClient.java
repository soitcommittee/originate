package com.traceflow.notificationservice.client;

import com.traceflow.notificationservice.dto.DebugSessionRequest;
import com.traceflow.notificationservice.exception.AlertConfigurationException;
import com.traceflow.notificationservice.exception.AlertDeliveryException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

@Component
public class HaalandDebugSessionClient {
    private final RestClient restClient;
    private final String apiBaseUrl;
    private final String authToken;

    public HaalandDebugSessionClient(RestClient.Builder builder,
                                     @Value("${haaland.api-base-url:}") String apiBaseUrl,
                                     @Value("${haaland.api-auth-token:}") String authToken) {
        this.restClient = builder.build();
        this.apiBaseUrl = stripTrailingSlash(apiBaseUrl);
        this.authToken = authToken;
    }

    HaalandDebugSessionClient(RestClient restClient, String apiBaseUrl, String authToken) {
        this.restClient = restClient;
        this.apiBaseUrl = stripTrailingSlash(apiBaseUrl);
        this.authToken = authToken;
    }

    public void create(DebugSessionRequest request) {
        if (apiBaseUrl.isBlank() || authToken.isBlank()) {
            throw new AlertConfigurationException("Haaland debug-session API is not configured");
        }
        try {
            RestClient.RequestBodySpec requestSpec = restClient.post()
                    .uri(apiBaseUrl + "/api/debug-sessions")
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + authToken);
            String requestId = MDC.get("requestId");
            if (requestId != null && !requestId.isBlank()) {
                requestSpec.header("X-Request-ID", requestId);
            }
            requestSpec.body(request)
                    .retrieve()
                    .toBodilessEntity();
        } catch (RestClientException ex) {
            throw new AlertDeliveryException("Haaland rejected the debug-session request", ex);
        }
    }

    private static String stripTrailingSlash(String value) {
        if (value == null) return "";
        return value.endsWith("/") ? value.substring(0, value.length() - 1) : value;
    }
}
