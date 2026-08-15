package com.traceflow.loanservice.client;

import java.math.BigDecimal;

public record ProductResponse(
        Long id,
        String code,
        String displayName,
        BigDecimal minAmount,
        BigDecimal maxAmount,
        Integer minTenureMonths,
        Integer maxTenureMonths,
        BigDecimal baseInterestRate,
        boolean active
) {
}
