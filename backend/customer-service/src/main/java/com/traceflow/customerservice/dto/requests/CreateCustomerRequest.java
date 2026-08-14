package com.traceflow.customerservice.dto.requests;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;

public record CreateCustomerRequest(
        @NotBlank String fullName,
        @NotBlank @Email String email,
        @NotBlank String phone,
        @NotNull @Positive BigDecimal monthlyIncome
) {
}

