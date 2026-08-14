package com.traceflow.loanservice.services;

import com.traceflow.loanservice.dto.requests.CreateLoanApplicationRequest;
import com.traceflow.loanservice.dto.requests.LoanDecisionRequest;
import com.traceflow.loanservice.dto.responses.LoanApplicationResponse;

import java.util.List;

public interface LoanApplicationService {
    LoanApplicationResponse create(CreateLoanApplicationRequest request);
    List<LoanApplicationResponse> getAll();
    LoanApplicationResponse getById(Long id);
    LoanApplicationResponse score(Long id);
    LoanApplicationResponse decide(Long id, LoanDecisionRequest request);
    LoanApplicationResponse accept(Long id);
    LoanApplicationResponse disburse(Long id);
}

