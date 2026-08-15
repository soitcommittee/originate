package com.traceflow.loanservice.services.impl;

import com.traceflow.loanservice.domain.LoanApplication;
import com.traceflow.loanservice.domain.LoanStatus;
import com.traceflow.loanservice.dto.responses.ApplicationStatusItemResponse;
import com.traceflow.loanservice.dto.responses.ApplicationStatusSnapshotResponse;
import com.traceflow.loanservice.repositories.LoanApplicationRepository;
import com.traceflow.loanservice.services.ApplicationStatusSnapshotService;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

@Service
public class ApplicationStatusSnapshotServiceImpl implements ApplicationStatusSnapshotService {
    private final LoanApplicationRepository repository;

    public ApplicationStatusSnapshotServiceImpl(LoanApplicationRepository repository) {
        this.repository = repository;
    }

    @Override
    @Transactional(readOnly = true)
    public ApplicationStatusSnapshotResponse capture() {
        List<LoanApplication> loans = repository.findAll(Sort.by(Sort.Direction.ASC, "id"));
        Map<LoanStatus, Long> counts = new EnumMap<>(LoanStatus.class);
        for (LoanStatus status : LoanStatus.values()) counts.put(status, 0L);
        loans.forEach(loan -> counts.merge(loan.getStatus(), 1L, Long::sum));

        List<ApplicationStatusItemResponse> applications = loans.stream()
                .map(loan -> new ApplicationStatusItemResponse(
                        loan.getId(), loan.getStatus(), loan.getUpdatedAt()))
                .toList();

        return new ApplicationStatusSnapshotResponse(
                Instant.now(), loans.size(), Map.copyOf(counts), applications);
    }
}
