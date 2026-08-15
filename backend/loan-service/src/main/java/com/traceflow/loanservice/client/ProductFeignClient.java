package com.traceflow.loanservice.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;

@FeignClient(name = "product-service", url = "${product-service.url}")
public interface ProductFeignClient {
    @GetMapping("/api/products/{code}")
    ProductResponse getByCode(@PathVariable("code") String code,
                              @RequestHeader("X-Request-ID") String requestId);
}
