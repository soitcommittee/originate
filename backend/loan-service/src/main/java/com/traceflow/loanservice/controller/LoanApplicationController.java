package com.traceflow.loanservice.controller;

import com.traceflow.loanservice.dto.requests.CreateLoanApplicationRequest;
import com.traceflow.loanservice.dto.requests.LoanDecisionRequest;
import com.traceflow.loanservice.dto.responses.LoanApplicationResponse;
import com.traceflow.loanservice.services.LoanApplicationService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/loan-applications")
@CrossOrigin(origins = "${app.cors.allowed-origin:http://localhost:5173}")
public class LoanApplicationController {
    private final LoanApplicationService loanService;

    public LoanApplicationController(LoanApplicationService loanService) {
        this.loanService = loanService;
    }

    @GetMapping
    public List<LoanApplicationResponse> getAll() {
        return loanService.getAll();
    }

    @GetMapping("/{id}")
    public LoanApplicationResponse getById(@PathVariable Long id) {
        return loanService.getById(id);
    }

    @PostMapping
    public ResponseEntity<LoanApplicationResponse> create(@Valid @RequestBody CreateLoanApplicationRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(loanService.create(request));
    }

    @PostMapping("/{id}/score")
    public LoanApplicationResponse score(@PathVariable Long id) {
        return loanService.score(id);
    }

    @PostMapping("/{id}/decision")
    public LoanApplicationResponse decide(@PathVariable Long id, @Valid @RequestBody LoanDecisionRequest request) {
        return loanService.decide(id, request);
    }

    @PostMapping("/{id}/accept")
    public LoanApplicationResponse accept(@PathVariable Long id) {
        return loanService.accept(id);
    }

    @PostMapping("/{id}/disburse")
    public LoanApplicationResponse disburse(@PathVariable Long id) {
        return loanService.disburse(id);
    }
}

