package com.traceflow.decisionservice.services.impl;

import com.traceflow.decisionservice.dto.DecisionRequest;
import com.traceflow.decisionservice.dto.DecisionResult;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class DecisionServiceImplTest {

    private final DecisionServiceImpl service = new DecisionServiceImpl();

    @Test
    void roundsMonthlyPaymentWhenNonTerminatingDecimalExpansion() {
        DecisionRequest request = new DecisionRequest(
                new BigDecimal("50000.00"),
                60,
                new BigDecimal("5000.00")
        );

        DecisionResult result = service.evaluate(request);

        assertNotNull(result);
        assertEquals(0, new BigDecimal("833.3333").compareTo(result.getMonthlyPayment()));
    }
}
