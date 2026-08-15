package com.traceflow.loanservice.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;

@FeignClient(name = "decision-service", url = "${decision-service.url}")
public interface DecisionFeignClient {
    @PostMapping("/api/decisions/evaluate")
    DecisionEvaluationResponse evaluate(@RequestBody DecisionEvaluationRequest request,
                                        @RequestHeader("X-Request-ID") String requestId);
}
