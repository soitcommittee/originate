package com.traceflow.decisionservice.services.impl;

import com.traceflow.decisionservice.domain.DecisionRecommendation;
import com.traceflow.decisionservice.dto.requests.DecisionEvaluationRequest;
import com.traceflow.decisionservice.repositories.InMemoryCreditPolicyRepository;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DecisionServiceImplTests {
    @Test
    void returnsEligibleRecommendationForAffordableApplication() {
        var response = service(false).evaluate(request(new BigDecimal("50000.00"), 60));

        assertThat(response.creditScore()).isEqualTo(760);
        assertThat(response.interestRate()).isEqualByComparingTo("4.25");
        assertThat(response.recommendation()).isEqualTo(DecisionRecommendation.ELIGIBLE);
    }

    @Test
    void declinesApplicationOutsideProductAmountLimit() {
        var response = service(false).evaluate(request(new BigDecimal("300000.00"), 60));

        assertThat(response.recommendation()).isEqualTo(DecisionRecommendation.DECLINE);
        assertThat(response.reason()).contains("amount");
    }

    @Test
    void simulatesDecisionServiceFailureWhenEnabled() {
        assertThatThrownBy(() -> service(true).evaluate(request(new BigDecimal("50000.00"), 60)))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Simulated decision service outage");
    }

    private DecisionServiceImpl service(boolean demoDecisionErrorEnabled) {
        return new DecisionServiceImpl(new InMemoryCreditPolicyRepository(), demoDecisionErrorEnabled);
    }

    private DecisionEvaluationRequest request(BigDecimal amount, int tenureMonths) {
        return new DecisionEvaluationRequest(1L, new BigDecimal("10000.00"), amount, tenureMonths,
                "PERSONAL_LOAN", new BigDecimal("5000.00"), new BigDecimal("250000.00"),
                12, 84, new BigDecimal("4.25"));
    }
}
