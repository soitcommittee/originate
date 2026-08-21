package com.traceflow.loanservice.client;

import java.math.BigDecimal;

public record PaymentProviderDisbursementRequest(String transactionId, BigDecimal amount, String currency) {

    public PaymentProviderDisbursementRequest(String transactionId, BigDecimal amount) {
        this(transactionId, amount, "USD");
    }
}
