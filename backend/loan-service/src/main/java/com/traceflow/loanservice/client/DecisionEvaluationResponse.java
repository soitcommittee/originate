package com.traceflow.loanservice.client;

import java.math.BigDecimal;
import java.time.Instant;

public record DecisionEvaluationResponse(
        Long customerId,
        String productCode,
        int creditScore,
        BigDecimal interestRate,
        String recommendation,
        String reason,
        Instant evaluatedAt
) {
}
