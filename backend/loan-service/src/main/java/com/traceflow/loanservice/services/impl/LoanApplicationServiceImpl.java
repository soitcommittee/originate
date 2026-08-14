package com.traceflow.loanservice.services.impl;

import com.traceflow.loanservice.client.CustomerFeignClient;
import com.traceflow.loanservice.client.CustomerResponse;
import com.traceflow.loanservice.client.DisbursementGatewayClient;
import com.traceflow.loanservice.domain.LoanApplication;
import com.traceflow.loanservice.domain.LoanStatus;
import com.traceflow.loanservice.dto.requests.CreateLoanApplicationRequest;
import com.traceflow.loanservice.dto.requests.LoanDecisionRequest;
import com.traceflow.loanservice.dto.responses.LoanApplicationResponse;
import com.traceflow.loanservice.exception.AppException;
import com.traceflow.loanservice.exception.ErrorCode;
import com.traceflow.loanservice.repositories.LoanApplicationRepository;
import com.traceflow.loanservice.services.LoanApplicationService;
import feign.FeignException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

@Service
public class LoanApplicationServiceImpl implements LoanApplicationService {
    private static final Logger log = LoggerFactory.getLogger(LoanApplicationServiceImpl.class);

    private final LoanApplicationRepository repository;
    private final CustomerFeignClient customerClient;
    private final DisbursementGatewayClient disbursementGatewayClient;

    public LoanApplicationServiceImpl(LoanApplicationRepository repository,
                                      CustomerFeignClient customerClient,
                                      DisbursementGatewayClient disbursementGatewayClient) {
        this.repository = repository;
        this.customerClient = customerClient;
        this.disbursementGatewayClient = disbursementGatewayClient;
    }

    @Override
    @Transactional
    public LoanApplicationResponse create(CreateLoanApplicationRequest request) {
        CustomerResponse customer = fetchCustomer(request.customerId());
        LoanApplication saved = repository.save(new LoanApplication(
                customer.id(), customer.fullName(), request.requestedAmount(), request.tenureMonths(), request.purpose()));
        log.info("loan_application_created loanApplicationId={} customerId={} amount={} tenureMonths={}",
                saved.getId(), saved.getCustomerId(), saved.getRequestedAmount(), saved.getTenureMonths());
        return toResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public List<LoanApplicationResponse> getAll() {
        return repository.findAll().stream().map(this::toResponse).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public LoanApplicationResponse getById(Long id) {
        return toResponse(find(id));
    }

    @Override
    @Transactional
    public LoanApplicationResponse score(Long id) {
        LoanApplication loan = find(id);
        requireStatus(loan, LoanStatus.SUBMITTED);
        CustomerResponse customer = fetchCustomer(loan.getCustomerId());

        BigDecimal annualIncome = customer.monthlyIncome().multiply(BigDecimal.valueOf(12));
        BigDecimal ratio = loan.getRequestedAmount().divide(annualIncome, 4, RoundingMode.HALF_UP);
        int score = ratio.compareTo(new BigDecimal("0.50")) <= 0 ? 760
                : ratio.compareTo(new BigDecimal("1.00")) <= 0 ? 690 : 610;
        BigDecimal interestRate = score >= 720 ? new BigDecimal("4.25")
                : score >= 650 ? new BigDecimal("6.75") : new BigDecimal("9.50");

        loan.scored(score, interestRate);
        log.info("loan_scored loanApplicationId={} customerId={} creditScore={} interestRate={}",
                loan.getId(), loan.getCustomerId(), score, interestRate);
        return toResponse(loan);
    }

    @Override
    @Transactional
    public LoanApplicationResponse decide(Long id, LoanDecisionRequest request) {
        LoanApplication loan = find(id);
        requireStatus(loan, LoanStatus.SCORED);
        LoanStatus decision;
        try {
            decision = LoanStatus.valueOf(request.decision().toUpperCase());
        } catch (IllegalArgumentException ex) {
            throw new AppException(ErrorCode.INVALID_DECISION, "Decision must be APPROVED or REJECTED", HttpStatus.BAD_REQUEST);
        }
        if (decision != LoanStatus.APPROVED && decision != LoanStatus.REJECTED) {
            throw new AppException(ErrorCode.INVALID_DECISION, "Decision must be APPROVED or REJECTED", HttpStatus.BAD_REQUEST);
        }
        loan.decided(decision, request.notes());
        log.info("loan_decision_recorded loanApplicationId={} decision={} creditScore={}",
                loan.getId(), decision, loan.getCreditScore());
        return toResponse(loan);
    }

    @Override
    @Transactional
    public LoanApplicationResponse accept(Long id) {
        LoanApplication loan = find(id);
        requireStatus(loan, LoanStatus.APPROVED);
        loan.accepted();
        log.info("loan_offer_accepted loanApplicationId={} customerId={}", loan.getId(), loan.getCustomerId());
        return toResponse(loan);
    }

    @Override
    @Transactional
    public LoanApplicationResponse disburse(Long id) {
        LoanApplication loan = find(id);
        requireStatus(loan, LoanStatus.ACCEPTED);
        log.info("loan_disbursement_started loanApplicationId={} customerId={} amount={}",
                loan.getId(), loan.getCustomerId(), loan.getRequestedAmount());
        disbursementGatewayClient.transferFunds(loan);
        loan.disbursed();
        log.info("loan_disbursement_completed loanApplicationId={} customerId={} amount={}",
                loan.getId(), loan.getCustomerId(), loan.getRequestedAmount());
        return toResponse(loan);
    }

    private CustomerResponse fetchCustomer(Long customerId) {
        try {
            return customerClient.getById(customerId, MDC.get("requestId"));
        } catch (FeignException ex) {
            throw new AppException(ErrorCode.CUSTOMER_SERVICE_UNAVAILABLE,
                    "Unable to validate customer " + customerId, HttpStatus.BAD_GATEWAY, ex);
        }
    }

    private LoanApplication find(Long id) {
        return repository.findById(id).orElseThrow(() -> new AppException(
                ErrorCode.LOAN_APPLICATION_NOT_FOUND, "Loan application " + id + " was not found", HttpStatus.NOT_FOUND));
    }

    private void requireStatus(LoanApplication loan, LoanStatus expected) {
        if (loan.getStatus() != expected) {
            throw new AppException(ErrorCode.INVALID_STATUS_TRANSITION,
                    "Loan application " + loan.getId() + " must be " + expected + " but is " + loan.getStatus(),
                    HttpStatus.CONFLICT);
        }
    }

    private LoanApplicationResponse toResponse(LoanApplication loan) {
        return new LoanApplicationResponse(loan.getId(), loan.getCustomerId(), loan.getApplicantName(),
                loan.getRequestedAmount(), loan.getTenureMonths(), loan.getPurpose(), loan.getStatus(),
                loan.getCreditScore(), loan.getInterestRate(), loan.getDecisionNotes(), loan.getCreatedAt(),
                loan.getUpdatedAt(), loan.getDisbursedAt());
    }
}

