package com.traceflow.decisionservice.services.impl;

import com.traceflow.decisionservice.dto.DecisionRequest;
import com.traceflow.decisionservice.services.DecisionService;
import java.math.BigDecimal;
import java.math.RoundingMode;
import org.springframework.stereotype.Service;

@Service
public class DecisionServiceImpl implements DecisionService {

    @Override
    public BigDecimal evaluate(DecisionRequest request) {
        BigDecimal monthlyPayment = request.requestedAmount()
                .divide(BigDecimal.valueOf(request.tenureMonths()), 4, RoundingMode.HALF_UP);
        return monthlyPayment;
    }
}
