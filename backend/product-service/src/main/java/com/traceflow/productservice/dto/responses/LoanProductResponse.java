package com.traceflow.productservice.dto.responses;

import java.math.BigDecimal;

public record LoanProductResponse(
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
