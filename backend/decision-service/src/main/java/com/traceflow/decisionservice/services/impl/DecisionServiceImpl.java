package com.traceflow.decisionservice.services.impl;

import com.traceflow.decisionservice.dto.DecisionRequest;
import com.traceflow.decisionservice.dto.DecisionResult;
import com.traceflow.decisionservice.services.DecisionService;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Service
public class DecisionServiceImpl implements DecisionService {

    @Override
    public DecisionResult evaluate(DecisionRequest request) {
        BigDecimal monthlyPayment = request.requestedAmount()
                .divide(BigDecimal.valueOf(request.tenureMonths()), 4, RoundingMode.HALF_UP);
        BigDecimal exposureRatio = monthlyPayment.divide(request.monthlyIncome(), 4, RoundingMode.HALF_UP);

        return new DecisionResult(monthlyPayment, exposureRatio);
    }
}
