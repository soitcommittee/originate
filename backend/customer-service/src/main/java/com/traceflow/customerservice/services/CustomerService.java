package com.traceflow.customerservice.services;

import com.traceflow.customerservice.dto.requests.CreateCustomerRequest;
import com.traceflow.customerservice.dto.responses.CustomerResponse;

import java.util.List;

public interface CustomerService {
    CustomerResponse create(CreateCustomerRequest request);
    CustomerResponse getById(Long id);
    List<CustomerResponse> getAll();
}

