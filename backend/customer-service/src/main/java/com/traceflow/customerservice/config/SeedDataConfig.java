package com.traceflow.customerservice.config;

import com.traceflow.customerservice.domain.Customer;
import com.traceflow.customerservice.repositories.CustomerRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.math.BigDecimal;

@Configuration
public class SeedDataConfig {
    @Bean
    CommandLineRunner seedCustomers(CustomerRepository repository) {
        return args -> {
            if (repository.count() == 0) {
                repository.save(new Customer("Aisha Rahman", "aisha@example.com", "+60 12-345 6789", new BigDecimal("8500.00")));
                repository.save(new Customer("Daniel Lim", "daniel@example.com", "+60 16-222 4311", new BigDecimal("6200.00")));
            }
        };
    }
}
