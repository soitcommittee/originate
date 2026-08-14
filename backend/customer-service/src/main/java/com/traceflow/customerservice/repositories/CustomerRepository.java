package com.traceflow.customerservice.repositories;

import com.traceflow.customerservice.domain.Customer;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CustomerRepository extends JpaRepository<Customer, Long> {
    boolean existsByEmailIgnoreCase(String email);
}

