package com.traceflow.decisionservice.services.impl;

import com.traceflow.decisionservice.services.DecisionService;
import com.traceflow.decisionservice.web.dto.DecisionRequestDTO;
import com.traceflow.decisionservice.web.dto.DecisionResponseDTO;
import org.springframework.stereotype.Service;
import java.math.BigDecimal;
import java.math.RoundingMode;

@Service
public class DecisionServiceImpl implements DecisionService {

    @Override
    public DecisionResponseDTO evaluate(DecisionRequestDTO request) {
        BigDecimal monthlyPayment = request.requestedAmount()
                .divide(BigDecimal.valueOf(request.tenureMonths()), 4, RoundingMode.HALF_UP);
        return new DecisionResponseDTO(monthlyPayment);
    }
}
