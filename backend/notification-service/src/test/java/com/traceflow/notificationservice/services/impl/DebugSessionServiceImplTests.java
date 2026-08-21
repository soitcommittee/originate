package com.traceflow.notificationservice.services.impl;

import com.traceflow.notificationservice.dto.PostmortemRequest;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

class DebugSessionServiceImplTests {
    @Test
    void createsPostmortemWithIncidentDetailsAndTemplateSections() throws Exception {
        var directory = Files.createTempDirectory("postmortems-");
        var service = new DebugSessionServiceImpl(directory.toString());

        var response = service.create(new PostmortemRequest(
                "loan-service", "INTERNAL_ERROR", "/api/loan-applications/15/disburse",
                "request-123", "2026-08-21T18:30:00Z", "Malformed provider timestamp"));

        String document = Files.readString(Path.of(response.documentPath()));
        assertThat(response.pic()).isEqualTo("29atly");
        assertThat(document).contains("## Summary", "## Solving process", "## Question analysis (timestamps)",
                "## Code difference", "## Future improvement", "Malformed provider timestamp");
        assertThat(document).contains("INTERNAL_ERROR", "request-123");
    }

}
