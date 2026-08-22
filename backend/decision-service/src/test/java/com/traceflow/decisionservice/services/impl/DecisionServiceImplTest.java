package com.traceflow.decisionservice.services.impl;

import com.traceflow.decisionservice.model.Decision;
import com.traceflow.decisionservice.model.DecisionRequest;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class DecisionServiceImplTest {

    private final DecisionServiceImpl service = new DecisionServiceImpl();

    @Test
    void evaluate_returnsRoundedMonthlyPaymentForNonTerminatingDivision() {
        DecisionRequest request = new DecisionRequest(
                new BigDecimal("50000"),
                new BigDecimal("5000"),
                60
        );

        Decision decision = assertDoesNotThrow(() -> service.evaluate(request));

        assertNotNull(decision);
        assertEquals(new BigDecimal("833.33"), decision.getMonthlyPayment());
        assertEquals(new BigDecimal("0.1667"), decision.getExposureRatio());
    }
}
