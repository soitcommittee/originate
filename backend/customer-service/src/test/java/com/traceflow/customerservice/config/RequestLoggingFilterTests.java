package com.traceflow.customerservice.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;

import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;

class RequestLoggingFilterTests {
    private final RequestLoggingFilter filter = new RequestLoggingFilter(new ObjectMapper(), 8192);

    @Test
    void masksSensitiveJsonWhileKeepingOperationalFields() {
        String body = """
                {"fullName":"Daniel Lim","email":"daniel@example.com","phone":"0123456789",
                 "monthlyIncome":9200,"requestedAmount":50000,"status":"SUBMITTED",
                 "nested":{"accessToken":"secret-token","creditScore":690}}
                """;

        String sanitized = filter.sanitizeBody(
                body.getBytes(StandardCharsets.UTF_8), "application/json", false);

        assertThat(sanitized)
                .doesNotContain("Daniel Lim", "daniel@example.com", "0123456789", "secret-token", "9200")
                .contains("D*******im", "d*****@example.com", "0*******89", "9**0", "***",
                        "requestedAmount", "50000", "SUBMITTED", "creditScore", "690");
    }

    @Test
    void excludesActuatorHealthChecksFromBusinessLogs() {
        MockHttpServletRequest healthRequest = new MockHttpServletRequest("GET", "/actuator/health/readiness");
        MockHttpServletRequest businessRequest = new MockHttpServletRequest("GET", "/api/customers");

        assertThat(filter.shouldNotFilter(healthRequest)).isTrue();
        assertThat(filter.shouldNotFilter(businessRequest)).isFalse();
    }
}
