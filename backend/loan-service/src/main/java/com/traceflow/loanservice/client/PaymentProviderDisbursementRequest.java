package com.traceflow.loanservice.client;

import java.math.BigDecimal;

public class PaymentProviderDisbursementRequest {

    private String transactionId;
    private BigDecimal amount;
    private String currency;

    public PaymentProviderDisbursementRequest() {
    }

    public PaymentProviderDisbursementRequest(String transactionId, BigDecimal amount) {
        this(transactionId, amount, "SGD");
    }

    public PaymentProviderDisbursementRequest(String transactionId, BigDecimal amount, String currency) {
        this.transactionId = transactionId;
        this.amount = amount;
        this.currency = currency;
    }

    public String getTransactionId() {
        return transactionId;
    }

    public void setTransactionId(String transactionId) {
        this.transactionId = transactionId;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public String getCurrency() {
        return currency;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }
}
