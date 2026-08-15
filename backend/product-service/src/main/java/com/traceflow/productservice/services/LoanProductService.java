package com.traceflow.productservice.services;

import com.traceflow.productservice.dto.responses.LoanProductResponse;

import java.util.List;

public interface LoanProductService {
    List<LoanProductResponse> getAll();

    LoanProductResponse getByCode(String code);
}
