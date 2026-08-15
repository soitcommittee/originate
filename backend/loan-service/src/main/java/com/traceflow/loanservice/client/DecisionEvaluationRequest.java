package com.traceflow.loanservice.client;

import java.math.BigDecimal;

public record DecisionEvaluationRequest(
        Long customerId,
        BigDecimal monthlyIncome,
        BigDecimal requestedAmount,
        Integer tenureMonths,
        String productCode,
        BigDecimal minAmount,
        BigDecimal maxAmount,
        Integer minTenureMonths,
        Integer maxTenureMonths,
        BigDecimal baseInterestRate
) {
}
