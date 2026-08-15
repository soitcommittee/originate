package com.traceflow.loanservice.dto.responses;

import com.traceflow.loanservice.domain.LoanStatus;

import java.time.Instant;

public record ApplicationStatusItemResponse(
        Long applicationId,
        LoanStatus status,
        Instant updatedAt
) {
}
