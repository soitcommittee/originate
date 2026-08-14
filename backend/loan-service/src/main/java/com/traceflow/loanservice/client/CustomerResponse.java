package com.traceflow.loanservice.client;

import java.math.BigDecimal;
import java.time.Instant;

public record CustomerResponse(
        Long id,
        String fullName,
        String email,
        String phone,
        BigDecimal monthlyIncome,
        Instant createdAt
) {
}

