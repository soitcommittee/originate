package com.traceflow.customerservice.services.impl;

import com.traceflow.customerservice.domain.Customer;
import com.traceflow.customerservice.dto.requests.CreateCustomerRequest;
import com.traceflow.customerservice.dto.responses.CustomerResponse;
import com.traceflow.customerservice.exception.AppException;
import com.traceflow.customerservice.exception.ErrorCode;
import com.traceflow.customerservice.repositories.CustomerRepository;
import com.traceflow.customerservice.services.CustomerService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class CustomerServiceImpl implements CustomerService {
    private static final Logger log = LoggerFactory.getLogger(CustomerServiceImpl.class);
    private final CustomerRepository customerRepository;

    public CustomerServiceImpl(CustomerRepository customerRepository) {
        this.customerRepository = customerRepository;
    }

    @Override
    @Transactional
    public CustomerResponse create(CreateCustomerRequest request) {
        if (customerRepository.existsByEmailIgnoreCase(request.email())) {
            throw new AppException(ErrorCode.EMAIL_ALREADY_EXISTS, "A customer with this email already exists", HttpStatus.CONFLICT);
        }
        Customer saved = customerRepository.save(new Customer(
                request.fullName(), request.email(), request.phone(), request.monthlyIncome()));
        log.info("customer_created customerId={}", saved.getId());
        return toResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public CustomerResponse getById(Long id) {
        return toResponse(find(id));
    }

    @Override
    @Transactional(readOnly = true)
    public List<CustomerResponse> getAll() {
        return customerRepository.findAll().stream().map(this::toResponse).toList();
    }

    private Customer find(Long id) {
        return customerRepository.findById(id).orElseThrow(() ->
                new AppException(ErrorCode.CUSTOMER_NOT_FOUND, "Customer " + id + " was not found", HttpStatus.NOT_FOUND));
    }

    private CustomerResponse toResponse(Customer customer) {
        return new CustomerResponse(customer.getId(), customer.getFullName(), customer.getEmail(),
                customer.getPhone(), customer.getMonthlyIncome(), customer.getCreatedAt());
    }
}

