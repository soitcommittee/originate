package com.traceflow.notificationservice.client;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.traceflow.notificationservice.exception.AlertDeliveryException;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManagerFactory;
import java.io.InputStream;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyStore;
import java.security.cert.Certificate;
import java.security.cert.CertificateFactory;
import java.time.Duration;
import java.util.Comparator;
import java.util.stream.StreamSupport;

@Component
public class KubernetesPodLogClient {
    private static final String SERVICE_ACCOUNT_DIRECTORY = "/var/run/secrets/kubernetes.io/serviceaccount/";

    private final ObjectMapper objectMapper;
    private final String apiBaseUrl;
    private final String namespace;
    private final Path tokenPath;
    private final Path caPath;
    private volatile HttpClient httpClient;

    public KubernetesPodLogClient(ObjectMapper objectMapper,
                                  @Value("${kubernetes.api-base-url:https://kubernetes.default.svc}") String apiBaseUrl,
                                  @Value("${kubernetes.workload-namespace:originate}") String namespace) {
        this(objectMapper, apiBaseUrl, namespace,
                Path.of(SERVICE_ACCOUNT_DIRECTORY + "token"),
                Path.of(SERVICE_ACCOUNT_DIRECTORY + "ca.crt"));
    }

    KubernetesPodLogClient(ObjectMapper objectMapper, String apiBaseUrl, String namespace,
                           Path tokenPath, Path caPath) {
        this.objectMapper = objectMapper;
        this.apiBaseUrl = apiBaseUrl;
        this.namespace = namespace;
        this.tokenPath = tokenPath;
        this.caPath = caPath;
    }

    public String getRecentLogs(String service) {
        if (service == null || !service.matches("[a-z0-9](?:[-a-z0-9]*[a-z0-9])?")) {
            throw new AlertDeliveryException("Alert contains an invalid Kubernetes service label", null);
        }
        try {
            String selector = URLEncoder.encode("app.kubernetes.io/name=" + service, StandardCharsets.UTF_8);
            JsonNode podList = objectMapper.readTree(get("/api/v1/namespaces/" + namespace
                    + "/pods?labelSelector=" + selector));
            JsonNode pod = StreamSupport.stream(podList.path("items").spliterator(), false)
                    .max(Comparator
                            .comparing((JsonNode item) -> isActive(item))
                            .thenComparing(item -> item.path("metadata").path("creationTimestamp").asText("")))
                    .orElseThrow(() -> new AlertDeliveryException(
                            "No Kubernetes pod found for service " + service, null));
            String podName = pod.path("metadata").path("name").asText();
            String container = StreamSupport.stream(pod.path("spec").path("containers").spliterator(), false)
                    .map(item -> item.path("name").asText())
                    .filter(service::equals)
                    .findFirst()
                    .orElseGet(() -> pod.path("spec").path("containers").path(0).path("name").asText());
            if (podName.isBlank() || container.isBlank()) {
                throw new AlertDeliveryException("Kubernetes returned incomplete pod metadata", null);
            }
            return get("/api/v1/namespaces/" + namespace + "/pods/" + encodePath(podName)
                    + "/log?container=" + URLEncoder.encode(container, StandardCharsets.UTF_8)
                    + "&timestamps=true&tailLines=400&sinceSeconds=900");
        } catch (AlertDeliveryException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new AlertDeliveryException("Unable to read recent Kubernetes pod logs", ex);
        }
    }

    private String get(String path) throws Exception {
        String token = Files.readString(tokenPath, StandardCharsets.UTF_8).trim();
        HttpRequest.Builder requestBuilder = HttpRequest.newBuilder(URI.create(apiBaseUrl + path))
                .timeout(Duration.ofSeconds(10))
                .header("Authorization", "Bearer " + token)
                .GET();
        String requestId = MDC.get("requestId");
        if (requestId != null && !requestId.isBlank()) {
            requestBuilder.header("X-Request-ID", requestId);
        }
        HttpRequest request = requestBuilder.build();
        HttpResponse<String> response = client().send(request,
                HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            throw new AlertDeliveryException(
                    "Kubernetes log API returned HTTP " + response.statusCode(), null);
        }
        return response.body();
    }

    private HttpClient client() throws Exception {
        if (httpClient != null) return httpClient;
        synchronized (this) {
            if (httpClient == null) {
                CertificateFactory certificateFactory = CertificateFactory.getInstance("X.509");
                Certificate certificate;
                try (InputStream input = Files.newInputStream(caPath)) {
                    certificate = certificateFactory.generateCertificate(input);
                }
                KeyStore trustStore = KeyStore.getInstance(KeyStore.getDefaultType());
                trustStore.load(null, null);
                trustStore.setCertificateEntry("kubernetes-ca", certificate);
                TrustManagerFactory trustManagers = TrustManagerFactory.getInstance(
                        TrustManagerFactory.getDefaultAlgorithm());
                trustManagers.init(trustStore);
                SSLContext sslContext = SSLContext.getInstance("TLS");
                sslContext.init(null, trustManagers.getTrustManagers(), null);
                httpClient = HttpClient.newBuilder()
                        .connectTimeout(Duration.ofSeconds(5))
                        .sslContext(sslContext)
                        .build();
            }
        }
        return httpClient;
    }

    private String encodePath(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8).replace("+", "%20");
    }

    private boolean isActive(JsonNode pod) {
        return "Running".equals(pod.path("status").path("phase").asText())
                && pod.path("metadata").path("deletionTimestamp").isMissingNode();
    }
}
