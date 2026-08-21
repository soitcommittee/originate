package com.traceflow.notificationservice.services.impl;

import com.traceflow.notificationservice.client.LarkWikiDocumentClient;
import com.traceflow.notificationservice.dto.DebugSessionResponse;
import com.traceflow.notificationservice.dto.PostmortemRequest;
import com.traceflow.notificationservice.services.DebugSessionService;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

@Service
public class DebugSessionServiceImpl implements DebugSessionService {
    private static final String PIC = "29atly";
    private final LarkWikiDocumentClient wikiClient;

    public DebugSessionServiceImpl(LarkWikiDocumentClient wikiClient) {
        this.wikiClient = wikiClient;
    }

    @Override
    public DebugSessionResponse create(PostmortemRequest request) {
        PostmortemRequest details = request == null ? new PostmortemRequest(null, null, null, null, null, null) : request;
        Instant createdAt = Instant.now().truncatedTo(ChronoUnit.SECONDS);
        String sessionId = "debug-" + UUID.randomUUID().toString().substring(0, 8);
        String service = value(details.service(), "unknown-service");
        String errorCode = value(details.errorCode(), "INTERNAL_ERROR");
        String endpoint = value(details.endpoint(), "not provided");
        String requestId = value(details.requestId(), "not provided");
        String observedAt = value(details.observedAt(), createdAt.toString());
        String markdown = render(sessionId, createdAt, service, errorCode, endpoint,
                requestId, observedAt, details.notes());
        String documentUrl = wikiClient.createDocument(service + " — " + errorCode + " — " + sessionId, markdown);

        return new DebugSessionResponse(sessionId, documentUrl, PIC, true);
    }

    private String render(String sessionId, Instant createdAt, String service, String errorCode,
                          String endpoint, String requestId, String observedAt, String notes) {
        String extraNotes = value(notes, "No additional notes were supplied.");
        return "# Postmortem: " + service + " — " + errorCode + "\n\n"
                + "- Session: " + sessionId + "\n"
                + "- PIC: " + PIC + "\n"
                + "- Status: Open\n"
                + "- Created at: " + createdAt + "\n"
                + "- Request ID: " + requestId + "\n\n"
                + "## Summary\n\n"
                + "A " + errorCode + " incident occurred in " + service + " while handling " + endpoint
                + ". The request was captured for investigation and follow-up.\n\n"
                + "## Solving process\n\n"
                + "1. Confirm the alert and correlate the request using the request ID.\n"
                + "2. Inspect the service logs and downstream responses around the incident time.\n"
                + "3. Reproduce the failing request in a controlled environment.\n"
                + "4. Identify the failing code path and apply a tested correction.\n"
                + "5. Redeploy, verify recovery, and monitor the error rate.\n\n"
                + "## Question analysis (timestamps)\n\n"
                + "| Timestamp | Question / observation | Analysis |\n"
                + "|---|---|---|\n"
                + "| " + observedAt + " | What happened? | " + errorCode + " was observed in " + service + ". |\n"
                + "| " + createdAt + " | Where did it happen? | Endpoint: " + endpoint + ". Request ID: " + requestId + ". |\n"
                + "| " + createdAt + " | What is the next action? | Reproduce, verify the fix, and confirm the alert clears. |\n\n"
                + "## Code difference\n\n"
                + "- Before: the failing input or dependency response was not handled safely.\n"
                + "- After: add validation, explicit error handling, structured logging, and a regression test.\n"
                + "- Notes: " + extraNotes + "\n\n"
                + "## Future improvement\n\n"
                + "- Add a regression test for the exact failure shape.\n"
                + "- Add a dashboard panel and alert annotation for " + errorCode + ".\n"
                + "- Document the rollback and recovery procedure.\n"
                + "- Assign a permanent owner and review the integration contract.\n";
    }

    private String value(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }
}
