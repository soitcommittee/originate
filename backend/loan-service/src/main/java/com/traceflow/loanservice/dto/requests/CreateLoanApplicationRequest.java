package com.traceflow.loanservice.dto.requests;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;

public record CreateLoanApplicationRequest(
        @NotNull @Positive Long customerId,
        @NotNull @Positive BigDecimal requestedAmount,
        @NotNull @Min(6) @Max(360) Integer tenureMonths,
        @NotBlank String purpose
) {
}

