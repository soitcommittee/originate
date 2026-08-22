package com.traceflow.decisionservice.services.impl;

import com.traceflow.decisionservice.model.DecisionEvaluationRequest;
import com.traceflow.decisionservice.model.DecisionEvaluationResponse;
import java.math.BigDecimal;
import java.math.RoundingMode;
import org.springframework.stereotype.Service;

@Service
public class DecisionServiceImpl {

    public DecisionEvaluationResponse evaluate(DecisionEvaluationRequest request) {
        BigDecimal monthlyPayment = request.requestedAmount()
                .divide(BigDecimal.valueOf(request.tenureMonths()), 2, RoundingMode.HALF_UP);
        BigDecimal exposureRatio = monthlyPayment.divide(request.monthlyIncome(), 4, RoundingMode.HALF_UP);

        if (exposureRatio.compareTo(BigDecimal.valueOf(0.35)) > 0) {
            return new DecisionEvaluationResponse("REJECTED", "Monthly payment exceeds 35% of income", monthlyPayment);
        }
        return new DecisionEvaluationResponse("APPROVED", "Monthly payment is within 35% of income", monthlyPayment);
    }
}
