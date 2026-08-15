package com.traceflow.customerservice.config;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.util.ContentCachingRequestWrapper;
import org.springframework.web.util.ContentCachingResponseWrapper;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class RequestLoggingFilter extends OncePerRequestFilter {
    private static final Logger log = LoggerFactory.getLogger(RequestLoggingFilter.class);
    private static final String REQUEST_ID_HEADER = "X-Request-ID";
    private static final String MASKED_SECRET = "***";
    private static final int MAX_HEADER_VALUE_LENGTH = 512;
    private static final Set<String> SECRET_KEYS = Set.of(
            "authorization", "proxyauthorization", "cookie", "setcookie", "xapikey",
            "password", "passwd", "secret", "token", "accesstoken", "refreshtoken"
    );
    private static final Set<String> PARTIALLY_MASKED_KEYS = Set.of(
            "xforwardedfor", "xrealip", "referer",
            "email", "phone", "phonenumber", "fullname", "applicantname",
            "monthlyincome", "annualincome",
            "nric", "identitynumber", "passportnumber", "bankaccount", "accountnumber",
            "cardnumber", "cvv", "pin", "address"
    );
    private static final Set<String> KEEP_LAST_ONE_KEYS = Set.of(
            "monthlyincome", "annualincome", "nric", "identitynumber", "passportnumber",
            "bankaccount", "accountnumber", "cardnumber", "cvv", "pin"
    );
    private static final Pattern SENSITIVE_QUERY_VALUE = Pattern.compile(
            "(?i)(password|passwd|secret|token|access_token|refresh_token|authorization|email|phone|full_name|applicant_name|nric|identity_number|account_number)=([^&]*)"
    );

    private final ObjectMapper objectMapper;
    private final int maxPayloadBytes;

    public RequestLoggingFilter(
            ObjectMapper objectMapper,
            @Value("${observability.http.max-payload-bytes:8192}") int maxPayloadBytes) {
        this.objectMapper = objectMapper;
        this.maxPayloadBytes = maxPayloadBytes;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !request.getRequestURI().startsWith("/api/");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        String requestId = request.getHeader(REQUEST_ID_HEADER);
        if (requestId == null || requestId.isBlank()) requestId = UUID.randomUUID().toString();

        ContentCachingRequestWrapper requestWrapper = new ContentCachingRequestWrapper(request, maxPayloadBytes);
        ContentCachingResponseWrapper responseWrapper = new ContentCachingResponseWrapper(response);
        long started = System.nanoTime();

        MDC.put("requestId", requestId);
        responseWrapper.setHeader(REQUEST_ID_HEADER, requestId);
        try {
            filterChain.doFilter(requestWrapper, responseWrapper);
        } finally {
            try {
                writeTransactionLog(requestWrapper, responseWrapper, started);
            } catch (RuntimeException loggingFailure) {
                log.warn("http_transaction_log_failed reason={}", loggingFailure.getMessage());
            } finally {
                responseWrapper.copyBodyToResponse();
                MDC.clear();
            }
        }
    }

    private void writeTransactionLog(ContentCachingRequestWrapper request,
                                     ContentCachingResponseWrapper response,
                                     long started) {
        long durationMs = (System.nanoTime() - started) / 1_000_000;
        int status = response.getStatus();
        String template = "http_transaction method={} path={} query={} status={} durationMs={} "
                + "requestHeaders={} requestBody={} responseHeaders={} responseBody={}";

        Object[] arguments = {
                request.getMethod(),
                request.getRequestURI(),
                sanitizeQuery(request.getQueryString()),
                status,
                durationMs,
                requestHeaders(request),
                sanitizeBody(request.getContentAsByteArray(), request.getContentType(), true),
                responseHeaders(response),
                sanitizeBody(response.getContentAsByteArray(), response.getContentType(), false)
        };

        if (status >= 500) {
            log.error(template, arguments);
        } else if (status >= 400) {
            log.warn(template, arguments);
        } else {
            log.info(template, arguments);
        }
    }

    private String requestHeaders(HttpServletRequest request) {
        Map<String, Object> headers = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
        Collections.list(request.getHeaderNames()).forEach(name ->
                headers.put(name, headerValue(name, Collections.list(request.getHeaders(name)))));
        return asJson(headers);
    }

    private String responseHeaders(HttpServletResponse response) {
        Map<String, Object> headers = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
        response.getHeaderNames().forEach(name ->
                headers.put(name, headerValue(name, List.copyOf(response.getHeaders(name)))));
        if (response.getContentType() != null) {
            headers.putIfAbsent("Content-Type", response.getContentType());
        }
        return asJson(headers);
    }

    private Object headerValue(String name, List<String> values) {
        if (isSecret(name)) return MASKED_SECRET;
        if (isSensitive(name)) {
            if (values.size() == 1) return maskValue(name, values.get(0));
            return values.stream().map(value -> maskValue(name, value)).toList();
        }
        if (values.size() == 1) return truncate(values.get(0), MAX_HEADER_VALUE_LENGTH);
        return values.stream().map(value -> truncate(value, MAX_HEADER_VALUE_LENGTH)).toList();
    }

    String sanitizeBody(byte[] payload, String contentType, boolean requestPayload) {
        if (payload.length == 0) return "-";
        if (requestPayload && payload.length >= maxPayloadBytes) {
            return "<payload-truncated bytesAtLeast=" + payload.length + ">";
        }
        if (contentType == null || !contentType.toLowerCase(Locale.ROOT).contains("json")) {
            return "<body-omitted contentType=" + contentType + " bytes=" + payload.length + ">";
        }

        try {
            JsonNode root = objectMapper.readTree(payload);
            mask(root);
            return truncate(objectMapper.writeValueAsString(root), maxPayloadBytes);
        } catch (IOException parseFailure) {
            return "<unparseable-json bytes=" + payload.length + ">";
        }
    }

    private void mask(JsonNode node) {
        if (node instanceof ObjectNode objectNode) {
            objectNode.properties().forEach(entry -> {
                if (isSecret(entry.getKey())) {
                    objectNode.put(entry.getKey(), MASKED_SECRET);
                } else if (isSensitive(entry.getKey())) {
                    objectNode.put(entry.getKey(), maskValue(entry.getKey(), entry.getValue().asText()));
                } else {
                    mask(entry.getValue());
                }
            });
        } else if (node instanceof ArrayNode arrayNode) {
            arrayNode.forEach(this::mask);
        }
    }

    private boolean isSensitive(String name) {
        String normalized = normalize(name);
        return PARTIALLY_MASKED_KEYS.contains(normalized) || isSecret(name);
    }

    private boolean isSecret(String name) {
        String normalized = normalize(name);
        return SECRET_KEYS.contains(normalized)
                || normalized.endsWith("password")
                || normalized.endsWith("secret")
                || normalized.endsWith("token");
    }

    private String normalize(String name) {
        return name.toLowerCase(Locale.ROOT).replace("-", "").replace("_", "");
    }

    private String maskValue(String key, String value) {
        if (value == null || value.isBlank()) return MASKED_SECRET;
        String normalized = normalize(key);
        if (normalized.equals("email")) return maskEmail(value);
        int visibleAtEnd = KEEP_LAST_ONE_KEYS.contains(normalized) ? 1 : 2;
        if (value.length() <= visibleAtEnd + 1) return "*".repeat(value.length());
        return value.charAt(0)
                + "*".repeat(value.length() - visibleAtEnd - 1)
                + value.substring(value.length() - visibleAtEnd);
    }

    private String maskEmail(String value) {
        int separator = value.indexOf('@');
        if (separator <= 0) return maskValue("fullname", value);
        return value.charAt(0) + "*".repeat(Math.max(1, separator - 1)) + value.substring(separator);
    }

    private String sanitizeQuery(String query) {
        if (query == null || query.isBlank()) return "-";
        return truncate(SENSITIVE_QUERY_VALUE.matcher(query).replaceAll(result -> Matcher.quoteReplacement(
                result.group(1) + "=" + (isSecret(result.group(1))
                        ? MASKED_SECRET
                        : maskValue(result.group(1), result.group(2))))), maxPayloadBytes);
    }

    private String asJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (IOException serializationFailure) {
            return "<serialization-failed>";
        }
    }

    private String truncate(String value, int limit) {
        if (value == null) return "-";
        if (value.length() <= limit) return value;
        return value.substring(0, limit) + "…[truncated]";
    }
}
