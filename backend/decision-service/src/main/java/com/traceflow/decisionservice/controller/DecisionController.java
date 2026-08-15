package com.traceflow.decisionservice.controller;

import com.traceflow.decisionservice.dto.requests.DecisionEvaluationRequest;
import com.traceflow.decisionservice.dto.responses.DecisionEvaluationResponse;
import com.traceflow.decisionservice.services.DecisionService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/decisions")
public class DecisionController {
    private final DecisionService decisionService;

    public DecisionController(DecisionService decisionService) {
        this.decisionService = decisionService;
    }

    @PostMapping("/evaluate")
    public DecisionEvaluationResponse evaluate(@Valid @RequestBody DecisionEvaluationRequest request) {
        return decisionService.evaluate(request);
    }
}
