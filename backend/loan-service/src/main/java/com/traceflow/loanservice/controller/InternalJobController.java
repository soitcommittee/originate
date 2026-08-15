package com.traceflow.loanservice.controller;

import com.traceflow.loanservice.dto.responses.ApplicationStatusSnapshotResponse;
import com.traceflow.loanservice.services.ApplicationStatusSnapshotService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/internal/jobs")
public class InternalJobController {
    private final ApplicationStatusSnapshotService snapshotService;

    public InternalJobController(ApplicationStatusSnapshotService snapshotService) {
        this.snapshotService = snapshotService;
    }

    @PostMapping("/application-status-snapshot")
    public ApplicationStatusSnapshotResponse captureApplicationStatusSnapshot() {
        return snapshotService.capture();
    }
}
