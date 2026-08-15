package com.traceflow.decisionservice.dto.responses;

import com.traceflow.decisionservice.domain.DecisionRecommendation;

import java.math.BigDecimal;
import java.time.Instant;

public record DecisionEvaluationResponse(
        Long customerId,
        String productCode,
        int creditScore,
        BigDecimal interestRate,
        DecisionRecommendation recommendation,
        String reason,
        Instant evaluatedAt
) {
}
