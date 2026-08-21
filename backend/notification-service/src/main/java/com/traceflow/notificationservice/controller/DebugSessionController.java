package com.traceflow.notificationservice.controller;

import com.traceflow.notificationservice.dto.DebugSessionResponse;
import com.traceflow.notificationservice.dto.PostmortemRequest;
import com.traceflow.notificationservice.services.DebugSessionService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/debug-sessions")
public class DebugSessionController {
    private final DebugSessionService debugSessionService;

    public DebugSessionController(DebugSessionService debugSessionService) {
        this.debugSessionService = debugSessionService;
    }

    @PostMapping
    public ResponseEntity<DebugSessionResponse> create(
            @RequestBody(required = false) PostmortemRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(debugSessionService.create(request));
    }
}
