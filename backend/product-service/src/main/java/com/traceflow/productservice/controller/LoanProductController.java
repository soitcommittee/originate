package com.traceflow.productservice.controller;

import com.traceflow.productservice.dto.responses.LoanProductResponse;
import com.traceflow.productservice.services.LoanProductService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/products")
public class LoanProductController {
    private final LoanProductService productService;

    public LoanProductController(LoanProductService productService) {
        this.productService = productService;
    }

    @GetMapping
    public List<LoanProductResponse> getAll() {
        return productService.getAll();
    }

    @GetMapping("/{code}")
    public LoanProductResponse getByCode(@PathVariable String code) {
        return productService.getByCode(code);
    }
}
