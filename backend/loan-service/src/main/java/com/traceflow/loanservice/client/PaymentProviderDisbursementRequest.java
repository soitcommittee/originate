package com.traceflow.loanservice.client;

import java.math.BigDecimal;

public record PaymentProviderDisbursementRequest(String transactionId, BigDecimal amount, String currency) {

    public PaymentProviderDisbursementRequest {
        if (transactionId == null || transactionId.isBlank()) {
            throw new IllegalArgumentException("transactionId must not be blank");
        }
        if (amount == null || amount.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("amount must be non-negative");
        }
        if (currency == null || !currency.matches("[A-Z]{3}")) {
            throw new IllegalArgumentException("currency must be a 3-letter uppercase ISO code");
        }
    }

    public PaymentProviderDisbursementRequest(String transactionId, BigDecimal amount) {
        this(transactionId, amount, "USD");
    }
}
