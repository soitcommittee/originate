package com.traceflow.decisionservice.services.impl;

import com.traceflow.decisionservice.models.Request;
import java.math.BigDecimal;
import java.math.RoundingMode;

public class DecisionServiceImpl {

    public BigDecimal evaluate(Request request) {
        BigDecimal monthlyPayment = request.requestedAmount()
                .divide(BigDecimal.valueOf(request.tenureMonths()), 4, RoundingMode.HALF_UP);
        return monthlyPayment;
    }
}
