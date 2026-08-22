package com.traceflow.decisionservice.services.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import com.traceflow.decisionservice.model.DecisionEvaluationRequest;
import com.traceflow.decisionservice.model.DecisionEvaluationResponse;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

class DecisionServiceImplTest {

    private final DecisionServiceImpl service = new DecisionServiceImpl();

    @Test
    void evaluate_withNonTerminatingDivision_returnsRoundedMonthlyPayment() {
        DecisionEvaluationRequest request = new DecisionEvaluationRequest(
                new BigDecimal("50000.00"),
                60,
                new BigDecimal("5000.00")
        );

        DecisionEvaluationResponse response = service.evaluate(request);

        assertNotNull(response);
        assertEquals(0, new BigDecimal("833.33").compareTo(response.monthlyPayment()));
        assertEquals("APPROVED", response.status());
    }
}
