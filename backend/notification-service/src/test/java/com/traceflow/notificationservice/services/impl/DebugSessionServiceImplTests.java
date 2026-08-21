package com.traceflow.notificationservice.services.impl;

import com.traceflow.notificationservice.client.LarkWikiDocumentClient;
import com.traceflow.notificationservice.dto.PostmortemRequest;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class DebugSessionServiceImplTests {
    @Test
    void createsPostmortemWithIncidentDetailsAndTemplateSections() throws Exception {
        var client = mock(LarkWikiDocumentClient.class);
        when(client.createDocument(org.mockito.ArgumentMatchers.contains("loan-service"),
                org.mockito.ArgumentMatchers.contains("Malformed provider timestamp")))
                .thenReturn("https://hjp4kdjoft7w.jp.larksuite.com/wiki/wikcn-test");
        var service = new DebugSessionServiceImpl(client);

        var response = service.create(new PostmortemRequest(
                "loan-service", "INTERNAL_ERROR", "/api/loan-applications/15/disburse",
                "request-123", "2026-08-21T18:30:00Z", "Malformed provider timestamp"));

        assertThat(response.pic()).isEqualTo("29atly");
        assertThat(response.larkDelivered()).isTrue();
        assertThat(response.documentPath()).contains("larksuite.com/wiki/");
    }

}
