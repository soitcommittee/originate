package com.traceflow.decisionservice.services.impl;

import com.traceflow.decisionservice.model.Decision;
import com.traceflow.decisionservice.model.DecisionRequest;
import com.traceflow.decisionservice.services.DecisionService;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Service
public class DecisionServiceImpl implements DecisionService {

    private static final BigDecimal MAX_EXPOSURE_RATIO = new BigDecimal("0.35");

    @Override
    public Decision evaluate(DecisionRequest request) {
        BigDecimal monthlyPayment = request.requestedAmount()
                .divide(BigDecimal.valueOf(request.tenureMonths()), 2, RoundingMode.HALF_UP);
        BigDecimal exposureRatio = monthlyPayment.divide(request.monthlyIncome(), 4, RoundingMode.HALF_UP);

        boolean approved = exposureRatio.compareTo(MAX_EXPOSURE_RATIO) <= 0;
        return new Decision(approved, monthlyPayment, exposureRatio);
    }
}
