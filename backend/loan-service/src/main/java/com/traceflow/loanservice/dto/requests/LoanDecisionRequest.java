package com.traceflow.loanservice.dto.requests;

import jakarta.validation.constraints.NotBlank;

public record LoanDecisionRequest(
        @NotBlank String decision,
        String notes
) {
}

