package com.traceflow.decisionservice.domain;

import java.math.BigDecimal;

public record CreditPolicy(
        BigDecimal lowRiskRatio,
        BigDecimal mediumRiskRatio,
        int lowRiskScore,
        int mediumRiskScore,
        int highRiskScore,
        BigDecimal mediumRiskPremium,
        BigDecimal highRiskPremium,
        int automaticEligibilityScore
) {
}
