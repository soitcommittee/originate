package com.traceflow.loanservice.dto.responses;

import com.traceflow.loanservice.domain.LoanStatus;

import java.time.Instant;
import java.util.List;
import java.util.Map;

public record ApplicationStatusSnapshotResponse(
        Instant capturedAt,
        long totalApplications,
        Map<LoanStatus, Long> statusCounts,
        List<ApplicationStatusItemResponse> applications
) {
}
