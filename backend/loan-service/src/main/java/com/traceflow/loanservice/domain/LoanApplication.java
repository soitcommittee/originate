package com.traceflow.loanservice.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "loan_applications")
public class LoanApplication {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long customerId;

    @Column(nullable = false)
    private String applicantName;

    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal requestedAmount;

    @Column(nullable = false)
    private Integer tenureMonths;

    @Column(nullable = false)
    private String purpose;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private LoanStatus status;

    private Integer creditScore;

    @Column(precision = 5, scale = 2)
    private BigDecimal interestRate;

    private String decisionNotes;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    @Column(nullable = false)
    private Instant updatedAt;

    private Instant disbursedAt;

    @Version
    private Long version;

    protected LoanApplication() {
    }

    public LoanApplication(Long customerId, String applicantName, BigDecimal requestedAmount,
                           Integer tenureMonths, String purpose) {
        this.customerId = customerId;
        this.applicantName = applicantName;
        this.requestedAmount = requestedAmount;
        this.tenureMonths = tenureMonths;
        this.purpose = purpose;
        this.status = LoanStatus.SUBMITTED;
        this.createdAt = Instant.now();
        this.updatedAt = this.createdAt;
    }

    public void scored(int creditScore, BigDecimal interestRate) {
        this.creditScore = creditScore;
        this.interestRate = interestRate;
        this.status = LoanStatus.SCORED;
        this.updatedAt = Instant.now();
    }

    public void decided(LoanStatus decision, String notes) {
        this.status = decision;
        this.decisionNotes = notes;
        this.updatedAt = Instant.now();
    }

    public void accepted() {
        this.status = LoanStatus.ACCEPTED;
        this.updatedAt = Instant.now();
    }

    public void disbursed() {
        this.status = LoanStatus.DISBURSED;
        this.disbursedAt = Instant.now();
        this.updatedAt = this.disbursedAt;
    }

    public Long getId() { return id; }
    public Long getCustomerId() { return customerId; }
    public String getApplicantName() { return applicantName; }
    public BigDecimal getRequestedAmount() { return requestedAmount; }
    public Integer getTenureMonths() { return tenureMonths; }
    public String getPurpose() { return purpose; }
    public LoanStatus getStatus() { return status; }
    public Integer getCreditScore() { return creditScore; }
    public BigDecimal getInterestRate() { return interestRate; }
    public String getDecisionNotes() { return decisionNotes; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public Instant getDisbursedAt() { return disbursedAt; }
}

