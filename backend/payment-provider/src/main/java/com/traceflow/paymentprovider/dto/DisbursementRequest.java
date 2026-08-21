package com.traceflow.paymentprovider.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record DisbursementRequest(
        @NotBlank String transactionId,
        @NotNull BigDecimal amount,
        String currency
) {
}
