package com.traceflow.notificationservice.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record DebugSessionRequest(
        @JsonProperty("repo_url") String repoUrl,
        @JsonProperty("service_name") String serviceName,
        @JsonProperty("log_text") String logText,
        @JsonProperty("base_ref") String baseRef
) {
}
