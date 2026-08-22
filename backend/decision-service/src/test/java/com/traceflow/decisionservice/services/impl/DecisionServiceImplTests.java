package com.traceflow.decisionservice.services.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.traceflow.decisionservice.model.Decision;
import com.traceflow.decisionservice.model.DecisionRequest;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

class DecisionServiceImplTests {

    private final DecisionServiceImpl service = new DecisionServiceImpl();

    @Test
    void roundsMonthlyPaymentWhenDivisionHasNonTerminatingDecimalExpansion() {
        DecisionRequest request = new DecisionRequest(
                new BigDecimal("50000.00"),
                60,
                new BigDecimal("5000.00"));

        Decision decision = service.evaluate(request);

        assertEquals(new BigDecimal("833.33"), decision.monthlyPayment());
    }
}
