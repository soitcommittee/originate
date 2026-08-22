package com.traceflow.decisionservice.services.impl;

import com.traceflow.decisionservice.model.Decision;
import com.traceflow.decisionservice.model.DecisionRequest;
import com.traceflow.decisionservice.services.DecisionService;
import java.math.BigDecimal;
import java.math.RoundingMode;
import org.springframework.stereotype.Service;

@Service
public class DecisionServiceImpl implements DecisionService {

    @Override
    public Decision evaluate(DecisionRequest request) {
        BigDecimal monthlyPayment = request.requestedAmount()
                .divide(BigDecimal.valueOf(request.tenureMonths()), 2, RoundingMode.HALF_UP);
        BigDecimal exposureRatio = monthlyPayment.divide(request.monthlyIncome(), 4, RoundingMode.HALF_UP);
        return new Decision(monthlyPayment, exposureRatio);
    }
}
