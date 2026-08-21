package com.traceflow.decisionservice.services.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.math.BigDecimal;

import org.junit.jupiter.api.Test;

import com.traceflow.decisionservice.models.DecisionRequest;
import com.traceflow.decisionservice.models.DecisionResponse;

class DecisionServiceImplTests {

    private final DecisionServiceImpl service = new DecisionServiceImpl();

    @Test
    void evaluateRoundsMonthlyPaymentForNonTerminatingDivision() {
        DecisionRequest request = new DecisionRequest(new BigDecimal("50000.00"), 60);
        DecisionResponse response = service.evaluate(request);
        assertEquals(new BigDecimal("833.33"), response.monthlyPayment());
    }
}
