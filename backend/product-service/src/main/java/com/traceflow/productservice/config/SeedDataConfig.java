package com.traceflow.productservice.config;

import com.traceflow.productservice.domain.LoanProduct;
import com.traceflow.productservice.repositories.LoanProductRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.math.BigDecimal;

@Configuration
public class SeedDataConfig {
    @Bean
    CommandLineRunner seedProducts(LoanProductRepository repository) {
        return args -> {
            if (repository.count() == 0) {
                repository.save(new LoanProduct("PERSONAL_LOAN", "Personal Financing",
                        new BigDecimal("5000.00"), new BigDecimal("250000.00"),
                        12, 84, new BigDecimal("4.25"), true));
                repository.save(new LoanProduct("HOME_RENOVATION", "Home Renovation Financing",
                        new BigDecimal("10000.00"), new BigDecimal("500000.00"),
                        12, 120, new BigDecimal("3.85"), true));
            }
        };
    }
}
