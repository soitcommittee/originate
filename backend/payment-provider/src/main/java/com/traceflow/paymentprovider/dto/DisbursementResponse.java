package com.traceflow.paymentprovider.dto;

import java.time.Instant;

public record DisbursementResponse(String providerReference, String apiVersion, Instant processedAt) {
}
