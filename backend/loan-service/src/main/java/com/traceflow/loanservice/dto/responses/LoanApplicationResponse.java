package com.traceflow.loanservice.dto.responses;

import com.traceflow.loanservice.domain.LoanStatus;

import java.math.BigDecimal;
import java.time.Instant;

public record LoanApplicationResponse(
        Long id,
        Long customerId,
        String applicantName,
        BigDecimal requestedAmount,
        Integer tenureMonths,
        String purpose,
        LoanStatus status,
        Integer creditScore,
        BigDecimal interestRate,
        String decisionNotes,
        Instant createdAt,
        Instant updatedAt,
        Instant disbursedAt
) {
}

