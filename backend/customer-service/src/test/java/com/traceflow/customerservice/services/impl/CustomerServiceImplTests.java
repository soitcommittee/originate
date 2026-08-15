package com.traceflow.customerservice.services.impl;

import com.traceflow.customerservice.domain.Customer;
import com.traceflow.customerservice.dto.requests.CreateCustomerRequest;
import com.traceflow.customerservice.exception.AppException;
import com.traceflow.customerservice.exception.ErrorCode;
import com.traceflow.customerservice.repositories.CustomerRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class CustomerServiceImplTests {
    private CustomerRepository repository;
    private CustomerServiceImpl service;

    @BeforeEach
    void setUp() {
        repository = mock(CustomerRepository.class);
        service = new CustomerServiceImpl(repository);
    }

    @Test
    void createsCustomerWhenEmailIsUnique() {
        when(repository.existsByEmailIgnoreCase("aisha@example.com")).thenReturn(false);
        when(repository.save(any(Customer.class))).thenAnswer(invocation -> {
            Customer customer = invocation.getArgument(0);
            ReflectionTestUtils.setField(customer, "id", 11L);
            return customer;
        });

        var response = service.create(request());

        assertThat(response.id()).isEqualTo(11L);
        assertThat(response.fullName()).isEqualTo("Aisha Rahman");
        verify(repository).save(any(Customer.class));
    }

    @Test
    void rejectsDuplicateEmail() {
        when(repository.existsByEmailIgnoreCase("aisha@example.com")).thenReturn(true);

        assertThatThrownBy(() -> service.create(request()))
                .isInstanceOfSatisfying(AppException.class,
                        error -> assertThat(error.getErrorCode()).isEqualTo(ErrorCode.EMAIL_ALREADY_EXISTS));
    }

    @Test
    void returnsCustomerByIdAndListsAllCustomers() {
        Customer customer = customer(11L);
        when(repository.findById(11L)).thenReturn(Optional.of(customer));
        when(repository.findAll()).thenReturn(List.of(customer));

        assertThat(service.getById(11L).email()).isEqualTo("aisha@example.com");
        assertThat(service.getAll()).extracting(response -> response.id()).containsExactly(11L);
    }

    @Test
    void reportsMissingCustomer() {
        when(repository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getById(99L))
                .isInstanceOfSatisfying(AppException.class,
                        error -> assertThat(error.getErrorCode()).isEqualTo(ErrorCode.CUSTOMER_NOT_FOUND));
    }

    private CreateCustomerRequest request() {
        return new CreateCustomerRequest("Aisha Rahman", "aisha@example.com",
                "+60 12-345 6789", new BigDecimal("8500.00"));
    }

    private Customer customer(Long id) {
        Customer customer = new Customer("Aisha Rahman", "aisha@example.com",
                "+60 12-345 6789", new BigDecimal("8500.00"));
        ReflectionTestUtils.setField(customer, "id", id);
        return customer;
    }
}
