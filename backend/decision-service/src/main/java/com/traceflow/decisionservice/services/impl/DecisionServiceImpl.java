package com.traceflow.decisionservice.services.impl;

import com.traceflow.decisionservice.models.DecisionRequest;
import com.traceflow.decisionservice.models.DecisionResponse;
import com.traceflow.decisionservice.services.DecisionService;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Service
public class DecisionServiceImpl implements DecisionService {

    @Override
    public DecisionResponse evaluate(DecisionRequest request) {
        BigDecimal monthlyPayment = request.requestedAmount()
                .divide(BigDecimal.valueOf(request.tenureMonths()), 2, RoundingMode.HALF_UP);
        BigDecimal totalPayment = monthlyPayment.multiply(BigDecimal.valueOf(request.tenureMonths()))
                .setScale(2, RoundingMode.HALF_UP);
        return new DecisionResponse(monthlyPayment, totalPayment);
    }
}
