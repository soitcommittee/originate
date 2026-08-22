package com.traceflow.decisionservice.services.impl;

import com.traceflow.decisionservice.model.Decision;
import com.traceflow.decisionservice.model.DecisionRequest;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;

class DecisionServiceImplTests {

    private final DecisionServiceImpl service = new DecisionServiceImpl();

    @Test
    void evaluateShouldRoundMonthlyPaymentWhenQuotientIsNonTerminating() {
        DecisionRequest request = new DecisionRequest(BigDecimal.valueOf(50000), 60);
        Decision decision = service.evaluate(request);

        assertEquals(0, decision.monthlyPayment().compareTo(new BigDecimal("833.33")));
    }
}
