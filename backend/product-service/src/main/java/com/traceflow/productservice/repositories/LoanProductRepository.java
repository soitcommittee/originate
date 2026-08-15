package com.traceflow.productservice.repositories;

import com.traceflow.productservice.domain.LoanProduct;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface LoanProductRepository extends JpaRepository<LoanProduct, Long> {
    Optional<LoanProduct> findByCodeIgnoreCase(String code);
}
