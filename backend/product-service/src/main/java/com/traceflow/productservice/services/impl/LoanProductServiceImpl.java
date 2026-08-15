package com.traceflow.productservice.services.impl;

import com.traceflow.productservice.domain.LoanProduct;
import com.traceflow.productservice.dto.responses.LoanProductResponse;
import com.traceflow.productservice.exception.AppException;
import com.traceflow.productservice.exception.ErrorCode;
import com.traceflow.productservice.repositories.LoanProductRepository;
import com.traceflow.productservice.services.LoanProductService;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class LoanProductServiceImpl implements LoanProductService {
    private final LoanProductRepository repository;

    public LoanProductServiceImpl(LoanProductRepository repository) {
        this.repository = repository;
    }

    @Override
    @Transactional(readOnly = true)
    public List<LoanProductResponse> getAll() {
        return repository.findAll().stream().map(this::toResponse).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public LoanProductResponse getByCode(String code) {
        LoanProduct product = repository.findByCodeIgnoreCase(code).orElseThrow(() -> new AppException(
                ErrorCode.PRODUCT_NOT_FOUND, "Loan product " + code + " was not found", HttpStatus.NOT_FOUND));
        if (!product.isActive()) {
            throw new AppException(ErrorCode.PRODUCT_INACTIVE,
                    "Loan product " + code + " is not active", HttpStatus.CONFLICT);
        }
        return toResponse(product);
    }

    private LoanProductResponse toResponse(LoanProduct product) {
        return new LoanProductResponse(product.getId(), product.getCode(), product.getDisplayName(),
                product.getMinAmount(), product.getMaxAmount(), product.getMinTenureMonths(),
                product.getMaxTenureMonths(), product.getBaseInterestRate(), product.isActive());
    }
}
