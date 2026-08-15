package com.traceflow.loanservice.services.impl;

import com.traceflow.loanservice.client.CustomerFeignClient;
import com.traceflow.loanservice.client.CustomerResponse;
import com.traceflow.loanservice.client.DecisionEvaluationRequest;
import com.traceflow.loanservice.client.DecisionEvaluationResponse;
import com.traceflow.loanservice.client.DecisionFeignClient;
import com.traceflow.loanservice.client.DisbursementGatewayClient;
import com.traceflow.loanservice.client.ProductFeignClient;
import com.traceflow.loanservice.client.ProductResponse;
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

import java.util.List;

@Service
public class LoanApplicationServiceImpl implements LoanApplicationService {
    private static final Logger log = LoggerFactory.getLogger(LoanApplicationServiceImpl.class);
    private static final String DEFAULT_PRODUCT_CODE = "PERSONAL_LOAN";

    private final LoanApplicationRepository repository;
    private final CustomerFeignClient customerClient;
    private final ProductFeignClient productClient;
    private final DecisionFeignClient decisionClient;
    private final DisbursementGatewayClient disbursementGatewayClient;

    public LoanApplicationServiceImpl(LoanApplicationRepository repository,
                                      CustomerFeignClient customerClient,
                                      ProductFeignClient productClient,
                                      DecisionFeignClient decisionClient,
                                      DisbursementGatewayClient disbursementGatewayClient) {
        this.repository = repository;
        this.customerClient = customerClient;
        this.productClient = productClient;
        this.decisionClient = decisionClient;
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
        ProductResponse product = fetchProduct(DEFAULT_PRODUCT_CODE);
        DecisionEvaluationResponse decision = evaluateDecision(loan, customer, product);

        loan.scored(decision.creditScore(), decision.interestRate());
        log.info("loan_scored loanApplicationId={} customerId={} productCode={} creditScore={} interestRate={} recommendation={} reason={}",
                loan.getId(), loan.getCustomerId(), decision.productCode(), decision.creditScore(),
                decision.interestRate(), decision.recommendation(), decision.reason());
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

    private ProductResponse fetchProduct(String productCode) {
        try {
            return productClient.getByCode(productCode, MDC.get("requestId"));
        } catch (FeignException ex) {
            throw new AppException(ErrorCode.PRODUCT_SERVICE_UNAVAILABLE,
                    "Unable to load product policy for " + productCode, HttpStatus.BAD_GATEWAY, ex);
        }
    }

    private DecisionEvaluationResponse evaluateDecision(LoanApplication loan, CustomerResponse customer,
                                                          ProductResponse product) {
        DecisionEvaluationRequest request = new DecisionEvaluationRequest(
                customer.id(), customer.monthlyIncome(), loan.getRequestedAmount(), loan.getTenureMonths(),
                product.code(), product.minAmount(), product.maxAmount(), product.minTenureMonths(),
                product.maxTenureMonths(), product.baseInterestRate());
        try {
            return decisionClient.evaluate(request, MDC.get("requestId"));
        } catch (FeignException ex) {
            throw new AppException(ErrorCode.DECISION_SERVICE_UNAVAILABLE,
                    "Unable to evaluate loan application " + loan.getId(), HttpStatus.BAD_GATEWAY, ex);
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
