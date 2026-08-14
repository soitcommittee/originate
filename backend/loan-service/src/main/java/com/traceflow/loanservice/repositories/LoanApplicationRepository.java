package com.traceflow.loanservice.repositories;

import com.traceflow.loanservice.domain.LoanApplication;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LoanApplicationRepository extends JpaRepository<LoanApplication, Long> {
}

