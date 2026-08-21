package com.traceflow.notificationservice.client;

import com.traceflow.notificationservice.exception.AlertConfigurationException;
import com.traceflow.notificationservice.exception.AlertDeliveryException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class LarkWikiDocumentClient {
    private static final Pattern WIKI_TOKEN = Pattern.compile("/wiki/([^/?#]+)");

    private final RestClient restClient;
    private final String baseUrl;
    private final String appId;
    private final String appSecret;
    private final String parentUrl;

    public LarkWikiDocumentClient(
            RestClient.Builder builder,
            @Value("${lark.wiki.base-url:https://open.larksuite.com}") String baseUrl,
            @Value("${lark.wiki.app-id:}") String appId,
            @Value("${lark.wiki.app-secret:}") String appSecret,
            @Value("${lark.wiki.parent-url:}") String parentUrl) {
        this.restClient = builder.build();
        this.baseUrl = stripTrailingSlash(baseUrl);
        this.appId = appId == null ? "" : appId;
        this.appSecret = appSecret == null ? "" : appSecret;
        this.parentUrl = parentUrl == null ? "" : parentUrl;
    }

    public String createDocument(String title, String markdown) {
        ensureConfigured();
        String accessToken = tenantAccessToken();
        String parentNodeToken = parentNodeToken();
        Map<?, ?> parentNode = getNode(accessToken, parentNodeToken);
        String spaceId = nestedString(parentNode, "data", "node", "space_id");
        Map<?, ?> createdNode = createWikiNode(accessToken, spaceId, parentNodeToken, title);
        String documentToken = nestedString(createdNode, "data", "node", "obj_token");
        String nodeToken = nestedString(createdNode, "data", "node", "node_token");
        appendTextBlocks(accessToken, documentToken, markdown);

        URI parent = URI.create(parentUrl);
        return parent.getScheme() + "://" + parent.getAuthority() + "/wiki/" + nodeToken;
    }

    private String tenantAccessToken() {
        try {
            Map<?, ?> response = restClient.post()
                    .uri(baseUrl + "/open-apis/auth/v3/tenant_access_token/internal")
                    .body(Map.of("app_id", appId, "app_secret", appSecret))
                    .retrieve().body(Map.class);
            ensureSuccess(response, "Lark authentication failed");
            return string(response, "tenant_access_token");
        } catch (RestClientException ex) {
            throw new AlertDeliveryException("Lark authentication request failed", ex);
        }
    }

    private Map<?, ?> getNode(String accessToken, String nodeToken) {
        try {
            return restClient.get()
                    .uri(baseUrl + "/open-apis/wiki/v2/spaces/get_node?token="
                            + URLEncoder.encode(nodeToken, StandardCharsets.UTF_8))
                    .header("Authorization", "Bearer " + accessToken)
                    .retrieve().body(Map.class);
        } catch (RestClientException ex) {
            throw new AlertDeliveryException("Lark wiki folder lookup failed", ex);
        }
    }

    private Map<?, ?> createWikiNode(String accessToken, String spaceId, String parentNodeToken, String title) {
        try {
            return restClient.post()
                    .uri(baseUrl + "/open-apis/wiki/v2/spaces/" + spaceId + "/nodes")
                    .header("Authorization", "Bearer " + accessToken)
                    .body(Map.of("obj_type", "docx", "node_type", "origin", "title", title,
                            "parent_node_token", parentNodeToken))
                    .retrieve().body(Map.class);
        } catch (RestClientException ex) {
            throw new AlertDeliveryException("Lark postmortem document creation failed", ex);
        }
    }

    private void appendTextBlocks(String accessToken, String documentToken, String markdown) {
        List<Map<String, Object>> children = new ArrayList<>();
        for (String line : markdown.split("\\R", -1)) {
            Map<String, Object> textRun = new LinkedHashMap<>();
            textRun.put("content", line.isEmpty() ? " " : line);
            Map<String, Object> element = new LinkedHashMap<>();
            element.put("text_run", textRun);
            Map<String, Object> text = new LinkedHashMap<>();
            text.put("elements", List.of(element));
            Map<String, Object> block = new LinkedHashMap<>();
            block.put("block_type", 2);
            block.put("text", text);
            children.add(block);
        }
        try {
            Map<?, ?> response = restClient.post()
                    .uri(baseUrl + "/open-apis/docx/v1/documents/" + documentToken
                            + "/blocks/" + documentToken + "/children?document_revision_id=-1")
                    .header("Authorization", "Bearer " + accessToken)
                    .body(Map.of("children", children, "index", 0))
                    .retrieve().body(Map.class);
            ensureSuccess(response, "Lark postmortem content upload failed");
        } catch (RestClientException ex) {
            throw new AlertDeliveryException("Lark postmortem content upload failed", ex);
        }
    }

    private String parentNodeToken() {
        Matcher matcher = WIKI_TOKEN.matcher(parentUrl);
        if (!matcher.find()) {
            throw new AlertConfigurationException("LARK_WIKI_PARENT_URL must contain /wiki/{node-token}");
        }
        return matcher.group(1);
    }

    private void ensureConfigured() {
        if (appId.isBlank() || appSecret.isBlank() || parentUrl.isBlank()) {
            throw new AlertConfigurationException("Lark wiki credentials or parent URL are not configured");
        }
    }

    private void ensureSuccess(Map<?, ?> response, String message) {
        Object code = response == null ? null : response.get("code");
        if (code == null || !"0".equals(code.toString())) {
            throw new AlertDeliveryException(message + ": " + response, null);
        }
    }

    private String string(Map<?, ?> response, String key) {
        Object value = response == null ? null : response.get(key);
        if (value == null || value.toString().isBlank()) {
            throw new AlertDeliveryException("Lark response did not contain " + key, null);
        }
        return value.toString();
    }

    private String nestedString(Map<?, ?> response, String first, String second, String third) {
        Object one = response == null ? null : response.get(first);
        Object two = one instanceof Map<?, ?> map ? map.get(second) : null;
        Object value = two instanceof Map<?, ?> map ? map.get(third) : null;
        if (value == null || value.toString().isBlank()) {
            ensureSuccess(response, "Lark API request failed");
            throw new AlertDeliveryException("Lark response did not contain " + third, null);
        }
        return value.toString();
    }

    private static String stripTrailingSlash(String value) {
        if (value == null || value.isBlank()) return "https://open.larksuite.com";
        return value.endsWith("/") ? value.substring(0, value.length() - 1) : value;
    }
}
