package com.traceflow.customerservice.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "customers")
public class Customer {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String fullName;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(nullable = false)
    private String phone;

    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal monthlyIncome;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    protected Customer() {
    }

    public Customer(String fullName, String email, String phone, BigDecimal monthlyIncome) {
        this.fullName = fullName;
        this.email = email;
        this.phone = phone;
        this.monthlyIncome = monthlyIncome;
        this.createdAt = Instant.now();
    }

    public Long getId() { return id; }
    public String getFullName() { return fullName; }
    public String getEmail() { return email; }
    public String getPhone() { return phone; }
    public BigDecimal getMonthlyIncome() { return monthlyIncome; }
    public Instant getCreatedAt() { return createdAt; }
}

