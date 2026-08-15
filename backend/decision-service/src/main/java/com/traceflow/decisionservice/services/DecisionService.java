package com.traceflow.decisionservice.services;

import com.traceflow.decisionservice.dto.requests.DecisionEvaluationRequest;
import com.traceflow.decisionservice.dto.responses.DecisionEvaluationResponse;

public interface DecisionService {
    DecisionEvaluationResponse evaluate(DecisionEvaluationRequest request);
}
