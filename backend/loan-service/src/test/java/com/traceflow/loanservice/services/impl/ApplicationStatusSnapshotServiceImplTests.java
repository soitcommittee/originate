package com.traceflow.loanservice.services.impl;

import com.traceflow.loanservice.domain.LoanApplication;
import com.traceflow.loanservice.domain.LoanStatus;
import com.traceflow.loanservice.repositories.LoanApplicationRepository;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Sort;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ApplicationStatusSnapshotServiceImplTests {
    private final LoanApplicationRepository repository = mock(LoanApplicationRepository.class);
    private final ApplicationStatusSnapshotServiceImpl service = new ApplicationStatusSnapshotServiceImpl(repository);

    @Test
    void returnsOneSanitizedBatchSnapshotForAllApplicationStatuses() {
        LoanApplication submitted = loan(1L);
        LoanApplication approved = loan(2L);
        approved.scored(690, new BigDecimal("6.75"));
        approved.decided(LoanStatus.APPROVED, "Passed policy checks");
        when(repository.findAll(any(Sort.class))).thenReturn(List.of(submitted, approved));

        var snapshot = service.capture();

        assertThat(snapshot.totalApplications()).isEqualTo(2);
        assertThat(snapshot.statusCounts()).containsEntry(LoanStatus.SUBMITTED, 1L)
                .containsEntry(LoanStatus.APPROVED, 1L)
                .containsEntry(LoanStatus.DISBURSED, 0L);
        assertThat(snapshot.applications())
                .extracting(item -> item.applicationId() + ":" + item.status())
                .containsExactly("1:SUBMITTED", "2:APPROVED");
    }

    private LoanApplication loan(Long id) {
        LoanApplication loan = new LoanApplication(
                7L, "Sensitive Applicant", new BigDecimal("50000.00"), 60, "Home renovation");
        ReflectionTestUtils.setField(loan, "id", id);
        return loan;
    }
}
