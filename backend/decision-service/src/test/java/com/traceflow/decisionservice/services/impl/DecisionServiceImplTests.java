package com.traceflow.decisionservice.services.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.traceflow.decisionservice.model.DecisionRequest;
import com.traceflow.decisionservice.model.DecisionResponse;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

class DecisionServiceImplTests {

    private final DecisionServiceImpl service = new DecisionServiceImpl();

    @Test
    void evaluate_withNonTerminatingDivision_returnsRoundedMonthlyPayment() {
        DecisionRequest request = new DecisionRequest(BigDecimal.valueOf(50000), 60);
        DecisionResponse response = service.evaluate(request);
        assertEquals(new BigDecimal("833.33"), response.monthlyPayment());
    }
}
