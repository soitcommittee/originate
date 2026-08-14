package com.traceflow.loanservice.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;

@FeignClient(name = "customer-service", url = "${customer-service.url}")
public interface CustomerFeignClient {
    @GetMapping("/api/customers/{id}")
    CustomerResponse getById(@PathVariable("id") Long id, @RequestHeader("X-Request-ID") String requestId);
}

