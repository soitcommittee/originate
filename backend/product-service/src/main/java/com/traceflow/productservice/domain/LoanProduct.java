package com.traceflow.productservice.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.math.BigDecimal;

@Entity
@Table(name = "loan_products")
public class LoanProduct {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 50)
    private String code;

    @Column(nullable = false)
    private String displayName;

    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal minAmount;

    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal maxAmount;

    @Column(nullable = false)
    private Integer minTenureMonths;

    @Column(nullable = false)
    private Integer maxTenureMonths;

    @Column(nullable = false, precision = 5, scale = 2)
    private BigDecimal baseInterestRate;

    @Column(nullable = false)
    private boolean active;

    protected LoanProduct() {
    }

    public LoanProduct(String code, String displayName, BigDecimal minAmount, BigDecimal maxAmount,
                       Integer minTenureMonths, Integer maxTenureMonths,
                       BigDecimal baseInterestRate, boolean active) {
        this.code = code;
        this.displayName = displayName;
        this.minAmount = minAmount;
        this.maxAmount = maxAmount;
        this.minTenureMonths = minTenureMonths;
        this.maxTenureMonths = maxTenureMonths;
        this.baseInterestRate = baseInterestRate;
        this.active = active;
    }

    public Long getId() { return id; }
    public String getCode() { return code; }
    public String getDisplayName() { return displayName; }
    public BigDecimal getMinAmount() { return minAmount; }
    public BigDecimal getMaxAmount() { return maxAmount; }
    public Integer getMinTenureMonths() { return minTenureMonths; }
    public Integer getMaxTenureMonths() { return maxTenureMonths; }
    public BigDecimal getBaseInterestRate() { return baseInterestRate; }
    public boolean isActive() { return active; }
}
