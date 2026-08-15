package com.traceflow.loanservice.services.impl;

import com.traceflow.loanservice.client.CustomerFeignClient;
import com.traceflow.loanservice.client.CustomerResponse;
import com.traceflow.loanservice.client.DecisionEvaluationResponse;
import com.traceflow.loanservice.client.DecisionFeignClient;
import com.traceflow.loanservice.client.DisbursementGatewayClient;
import com.traceflow.loanservice.client.ProductFeignClient;
import com.traceflow.loanservice.client.ProductResponse;
import com.traceflow.loanservice.domain.LoanApplication;
import com.traceflow.loanservice.domain.LoanStatus;
import com.traceflow.loanservice.dto.requests.CreateLoanApplicationRequest;
import com.traceflow.loanservice.dto.requests.LoanDecisionRequest;
import com.traceflow.loanservice.exception.AppException;
import com.traceflow.loanservice.exception.ErrorCode;
import com.traceflow.loanservice.repositories.LoanApplicationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class LoanApplicationServiceImplTests {
    private LoanApplicationRepository repository;
    private CustomerFeignClient customerClient;
    private ProductFeignClient productClient;
    private DecisionFeignClient decisionClient;
    private DisbursementGatewayClient disbursementClient;
    private LoanApplicationServiceImpl service;

    @BeforeEach
    void setUp() {
        repository = mock(LoanApplicationRepository.class);
        customerClient = mock(CustomerFeignClient.class);
        productClient = mock(ProductFeignClient.class);
        decisionClient = mock(DecisionFeignClient.class);
        disbursementClient = mock(DisbursementGatewayClient.class);
        service = new LoanApplicationServiceImpl(
                repository, customerClient, productClient, decisionClient, disbursementClient);
    }

    @Test
    void createsAndReadsLoanApplications() {
        when(customerClient.getById(any(), any())).thenReturn(customer());
        when(repository.save(any(LoanApplication.class))).thenAnswer(invocation -> {
            LoanApplication loan = invocation.getArgument(0);
            ReflectionTestUtils.setField(loan, "id", 1L);
            return loan;
        });

        var created = service.create(new CreateLoanApplicationRequest(
                7L, new BigDecimal("50000.00"), 60, "Home renovation"));
        when(repository.findById(1L)).thenReturn(Optional.of(loan(1L)));
        when(repository.findAll()).thenReturn(List.of(loan(1L)));

        assertThat(created.status()).isEqualTo(LoanStatus.SUBMITTED);
        assertThat(service.getById(1L).id()).isEqualTo(1L);
        assertThat(service.getAll()).hasSize(1);
    }

    @Test
    void delegatesScoringToProductAndDecisionServices() {
        LoanApplication loan = loan(1L);
        when(repository.findById(1L)).thenReturn(Optional.of(loan));
        when(customerClient.getById(any(), any())).thenReturn(customer());
        when(productClient.getByCode(anyString(), any())).thenReturn(product());
        when(decisionClient.evaluate(any(), any())).thenReturn(new DecisionEvaluationResponse(
                7L, "PERSONAL_LOAN", 690, new BigDecimal("6.75"), "ELIGIBLE",
                "Affordability and product policy checks passed", Instant.now()));

        var response = service.score(1L);

        assertThat(response.creditScore()).isEqualTo(690);
        assertThat(response.interestRate()).isEqualByComparingTo("6.75");
        verify(productClient).getByCode(anyString(), any());
        verify(decisionClient).evaluate(any(), any());
    }

    @Test
    void recordsApprovalAndAcceptance() {
        LoanApplication loan = scoredLoan(1L);
        when(repository.findById(1L)).thenReturn(Optional.of(loan));

        var approved = service.decide(1L, new LoanDecisionRequest("APPROVED", "Policy passed"));
        var accepted = service.accept(1L);

        assertThat(approved.status()).isEqualTo(LoanStatus.APPROVED);
        assertThat(accepted.status()).isEqualTo(LoanStatus.ACCEPTED);
    }

    @Test
    void rejectsUnknownAndUnsupportedDecisionValues() {
        when(repository.findById(1L)).thenReturn(Optional.of(scoredLoan(1L)));
        assertThatThrownBy(() -> service.decide(1L, new LoanDecisionRequest("MAYBE", "")))
                .isInstanceOfSatisfying(AppException.class,
                        error -> assertThat(error.getErrorCode()).isEqualTo(ErrorCode.INVALID_DECISION));

        when(repository.findById(2L)).thenReturn(Optional.of(scoredLoan(2L)));
        assertThatThrownBy(() -> service.decide(2L, new LoanDecisionRequest("SUBMITTED", "")))
                .isInstanceOfSatisfying(AppException.class,
                        error -> assertThat(error.getErrorCode()).isEqualTo(ErrorCode.INVALID_DECISION));
    }

    @Test
    void disbursesAcceptedApplication() {
        LoanApplication loan = scoredLoan(1L);
        loan.decided(LoanStatus.APPROVED, "Policy passed");
        loan.accepted();
        when(repository.findById(1L)).thenReturn(Optional.of(loan));

        var response = service.disburse(1L);

        assertThat(response.status()).isEqualTo(LoanStatus.DISBURSED);
        assertThat(response.disbursedAt()).isNotNull();
        verify(disbursementClient).transferFunds(loan);
    }

    @Test
    void rejectsMissingApplicationAndInvalidWorkflowTransition() {
        when(repository.findById(99L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.getById(99L))
                .isInstanceOfSatisfying(AppException.class,
                        error -> assertThat(error.getErrorCode()).isEqualTo(ErrorCode.LOAN_APPLICATION_NOT_FOUND));

        when(repository.findById(1L)).thenReturn(Optional.of(loan(1L)));
        assertThatThrownBy(() -> service.accept(1L))
                .isInstanceOfSatisfying(AppException.class,
                        error -> assertThat(error.getErrorCode()).isEqualTo(ErrorCode.INVALID_STATUS_TRANSITION));
    }

    private CustomerResponse customer() {
        return new CustomerResponse(7L, "Daniel Lim", "daniel@example.com", "+60 16-222 4311",
                new BigDecimal("6200.00"), Instant.now());
    }

    private ProductResponse product() {
        return new ProductResponse(1L, "PERSONAL_LOAN", "Personal Financing",
                new BigDecimal("5000.00"), new BigDecimal("250000.00"),
                12, 84, new BigDecimal("4.25"), true);
    }

    private LoanApplication loan(Long id) {
        LoanApplication loan = new LoanApplication(
                7L, "Daniel Lim", new BigDecimal("50000.00"), 60, "Home renovation");
        ReflectionTestUtils.setField(loan, "id", id);
        return loan;
    }

    private LoanApplication scoredLoan(Long id) {
        LoanApplication loan = loan(id);
        loan.scored(690, new BigDecimal("6.75"));
        return loan;
    }
}
