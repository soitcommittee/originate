package com.traceflow.decisionservice.services.impl;

import com.traceflow.decisionservice.model.Decision;
import com.traceflow.decisionservice.model.DecisionRequest;
import com.traceflow.decisionservice.services.DecisionService;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Service
public class DecisionServiceImpl implements DecisionService {

    @Override
    public Decision evaluate(DecisionRequest request) {
        BigDecimal monthlyPayment = request.requestedAmount()
                .divide(BigDecimal.valueOf(request.tenureMonths()), 2, RoundingMode.HALF_UP);
        BigDecimal totalPayment = monthlyPayment.multiply(BigDecimal.valueOf(request.tenureMonths()));
        BigDecimal interest = totalPayment.subtract(request.requestedAmount());
        return new Decision(monthlyPayment, totalPayment, interest);
    }
}
