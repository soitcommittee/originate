package com.traceflow.decisionservice.dto.requests;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;

public record DecisionEvaluationRequest(
        @NotNull @Positive Long customerId,
        @NotNull @DecimalMin("0.01") BigDecimal monthlyIncome,
        @NotNull @DecimalMin("0.01") BigDecimal requestedAmount,
        @NotNull @Positive Integer tenureMonths,
        @NotBlank String productCode,
        @NotNull @DecimalMin("0.00") BigDecimal minAmount,
        @NotNull @DecimalMin("0.01") BigDecimal maxAmount,
        @NotNull @Positive Integer minTenureMonths,
        @NotNull @Positive Integer maxTenureMonths,
        @NotNull @DecimalMin("0.00") BigDecimal baseInterestRate
) {
}
