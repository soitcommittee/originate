package com.traceflow.loanservice.client;

import java.math.BigDecimal;

public record PaymentProviderDisbursementRequest(String transactionId, BigDecimal amount, String currency) {

    public PaymentProviderDisbursementRequest(String transactionId, BigDecimal amount) {
        this(transactionId, amount, "USD");
    }

    public PaymentProviderDisbursementRequest {
        if (currency == null || !currency.matches("^[A-Z]{3}$")) {
            throw new IllegalArgumentException("currency must be a 3-letter ISO code");
        }
    }
}
