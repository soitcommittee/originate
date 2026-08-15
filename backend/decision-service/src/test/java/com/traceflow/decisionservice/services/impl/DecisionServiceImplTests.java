package com.traceflow.decisionservice.services.impl;

import com.traceflow.decisionservice.domain.DecisionRecommendation;
import com.traceflow.decisionservice.dto.requests.DecisionEvaluationRequest;
import com.traceflow.decisionservice.repositories.InMemoryCreditPolicyRepository;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

class DecisionServiceImplTests {
    private final DecisionServiceImpl service = new DecisionServiceImpl(new InMemoryCreditPolicyRepository());

    @Test
    void returnsEligibleRecommendationForAffordableApplication() {
        var response = service.evaluate(request(new BigDecimal("50000.00"), 60));

        assertThat(response.creditScore()).isEqualTo(760);
        assertThat(response.interestRate()).isEqualByComparingTo("4.25");
        assertThat(response.recommendation()).isEqualTo(DecisionRecommendation.ELIGIBLE);
    }

    @Test
    void declinesApplicationOutsideProductAmountLimit() {
        var response = service.evaluate(request(new BigDecimal("300000.00"), 60));

        assertThat(response.recommendation()).isEqualTo(DecisionRecommendation.DECLINE);
        assertThat(response.reason()).contains("amount");
    }

    private DecisionEvaluationRequest request(BigDecimal amount, int tenureMonths) {
        return new DecisionEvaluationRequest(1L, new BigDecimal("10000.00"), amount, tenureMonths,
                "PERSONAL_LOAN", new BigDecimal("5000.00"), new BigDecimal("250000.00"),
                12, 84, new BigDecimal("4.25"));
    }
}
