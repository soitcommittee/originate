package com.traceflow.decisionservice.services.impl;

import com.traceflow.decisionservice.model.DecisionRequest;
import com.traceflow.decisionservice.model.DecisionResponse;
import com.traceflow.decisionservice.services.DecisionService;
import java.math.BigDecimal;
import java.math.RoundingMode;
import org.springframework.stereotype.Service;

@Service
public class DecisionServiceImpl implements DecisionService {

    @Override
    public DecisionResponse evaluate(DecisionRequest request) {
        BigDecimal monthlyPayment = request.requestedAmount()
                .divide(BigDecimal.valueOf(request.tenureMonths()), 2, RoundingMode.HALF_UP);
        return new DecisionResponse(monthlyPayment);
    }
}
