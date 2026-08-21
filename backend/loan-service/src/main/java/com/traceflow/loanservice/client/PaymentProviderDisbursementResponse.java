package com.traceflow.loanservice.client;

import java.time.Instant;

public record PaymentProviderDisbursementResponse(String providerReference, String apiVersion, Instant processedAt) {
}
