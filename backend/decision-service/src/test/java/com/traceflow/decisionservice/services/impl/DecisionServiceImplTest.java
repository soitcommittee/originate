package com.traceflow.decisionservice.services.impl;

import com.traceflow.decisionservice.dto.DecisionRequest;
import com.traceflow.decisionservice.dto.DecisionResponse;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;

class DecisionServiceImplTest {

    private final DecisionServiceImpl service = new DecisionServiceImpl();

    @Test
    void failsWhenMonthlyPaymentHasNonTerminatingDecimalExpansion() {
        DecisionRequest request = new DecisionRequest(BigDecimal.TEN, 3);

        DecisionResponse response = service.evaluate(request);

        assertEquals(new BigDecimal("3.3333"), response.getMonthlyPayment());
    }
}
