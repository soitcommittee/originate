package com.traceflow.notificationservice.config;

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
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;
import java.util.UUID;

@Component
public class RequestLoggingFilter extends OncePerRequestFilter {
    private static final Logger log = LoggerFactory.getLogger(RequestLoggingFilter.class);
    private static final String REQUEST_ID_HEADER = "X-Request-ID";

    private final ObjectMapper objectMapper;
    private final int maxPayloadBytes;

    public RequestLoggingFilter(ObjectMapper objectMapper,
                                @Value("${observability.http.max-payload-bytes:16384}") int maxPayloadBytes) {
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
        ContentCachingRequestWrapper cachedRequest = new ContentCachingRequestWrapper(request, maxPayloadBytes);
        ContentCachingResponseWrapper cachedResponse = new ContentCachingResponseWrapper(response);
        long started = System.nanoTime();

        MDC.put("requestId", requestId);
        cachedResponse.setHeader(REQUEST_ID_HEADER, requestId);
        try {
            filterChain.doFilter(cachedRequest, cachedResponse);
        } finally {
            int status = cachedResponse.getStatus();
            long durationMs = (System.nanoTime() - started) / 1_000_000;
            String template = "http_transaction method={} path={} status={} durationMs={} requestHeaders={} requestBody={} responseBody={}";
            Object[] arguments = {request.getMethod(), request.getRequestURI(), status, durationMs,
                    headers(request), body(cachedRequest.getContentAsByteArray()), body(cachedResponse.getContentAsByteArray())};
            if (status >= 500) log.error(template, arguments);
            else if (status >= 400) log.warn(template, arguments);
            else log.info(template, arguments);
            cachedResponse.copyBodyToResponse();
            MDC.clear();
        }
    }

    private String headers(HttpServletRequest request) {
        Map<String, String> headers = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
        Collections.list(request.getHeaderNames()).forEach(name -> {
            String normalized = name.toLowerCase(Locale.ROOT);
            String value = normalized.contains("authorization") || normalized.contains("cookie")
                    || normalized.contains("token") || normalized.contains("secret")
                    ? "***" : request.getHeader(name);
            headers.put(name, value);
        });
        return write(headers);
    }

    private String body(byte[] content) {
        if (content.length == 0) return "-";
        if (content.length > maxPayloadBytes) return "<payload-truncated bytes=" + content.length + ">";
        try {
            JsonNode node = objectMapper.readTree(content);
            maskSecrets(node);
            return write(node);
        } catch (IOException ignored) {
            return "<non-json bytes=" + content.length + ">";
        }
    }

    private void maskSecrets(JsonNode node) {
        if (node instanceof ObjectNode objectNode) {
            objectNode.properties().forEach(entry -> {
                String key = entry.getKey().toLowerCase(Locale.ROOT);
                if (key.contains("token") || key.contains("secret") || key.contains("password") || key.contains("webhook")) {
                    objectNode.put(entry.getKey(), "***");
                } else {
                    maskSecrets(entry.getValue());
                }
            });
        } else if (node instanceof ArrayNode arrayNode) {
            arrayNode.forEach(this::maskSecrets);
        }
    }

    private String write(Object value) {
        try {
            String json = objectMapper.writeValueAsString(value);
            return json.length() <= maxPayloadBytes ? json : json.substring(0, maxPayloadBytes) + "…[truncated]";
        } catch (IOException ex) {
            return "<serialization-failed>";
        }
    }
}
