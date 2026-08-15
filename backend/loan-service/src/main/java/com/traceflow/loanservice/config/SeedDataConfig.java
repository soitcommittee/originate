package com.traceflow.loanservice.config;

import com.traceflow.loanservice.domain.LoanApplication;
import com.traceflow.loanservice.domain.LoanStatus;
import com.traceflow.loanservice.repositories.LoanApplicationRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.math.BigDecimal;
import java.util.List;

@Configuration
public class SeedDataConfig {
    private static final String APPROVAL_NOTES = "Affordability, identity and credit policy checks passed.";

    @Bean
    CommandLineRunner seedApplications(LoanApplicationRepository repository) {
        return args -> {
            if (repository.count() == 0) {
                repository.saveAll(List.of(
                        submitted(1L, "Aisha Rahman", "50000", 60, "Home renovation"),
                        submitted(8L, "Kelvin Lee", "18000", 36, "Professional certification"),
                        scored(2L, "Daniel Lim", "35000", 60, "Debt consolidation", 690, "6.75"),
                        scored(5L, "Nur Izzati Hassan", "22000", 48, "Education expenses", 724, "5.40"),
                        approved(3L, "Mei Ling Tan", "85000", 84, "Property refurbishment", 781, "4.15"),
                        approved(9L, "Priya Menon", "120000", 120, "Investment property", 758, "4.45"),
                        accepted(4L, "Arjun Nair", "65000", 72, "Business equipment", 742, "4.95"),
                        disbursed(6L, "Jason Wong", "150000", 120, "Home extension", 803, "3.95"),
                        disbursed(7L, "Siti Hajar Ahmad", "28000", 48, "Medical expenses", 716, "5.60"),
                        disbursed(11L, "Amanda Goh", "45000", 60, "Vehicle financing", 769, "4.30"),
                        rejected(10L, "Farid Iskandar", "95000", 84, "Working capital", 602, "9.80"),
                        rejected(12L, "Hafiz Zulkifli", "40000", 60, "Personal financing", 571, "11.25")
                ));
            }
        };
    }

    private LoanApplication submitted(Long customerId, String name, String amount, int tenure, String purpose) {
        return new LoanApplication(customerId, name, new BigDecimal(amount), tenure, purpose);
    }

    private LoanApplication scored(Long customerId, String name, String amount, int tenure, String purpose,
                                   int score, String rate) {
        LoanApplication loan = submitted(customerId, name, amount, tenure, purpose);
        loan.scored(score, new BigDecimal(rate));
        return loan;
    }

    private LoanApplication approved(Long customerId, String name, String amount, int tenure, String purpose,
                                     int score, String rate) {
        LoanApplication loan = scored(customerId, name, amount, tenure, purpose, score, rate);
        loan.decided(LoanStatus.APPROVED, APPROVAL_NOTES);
        return loan;
    }

    private LoanApplication accepted(Long customerId, String name, String amount, int tenure, String purpose,
                                     int score, String rate) {
        LoanApplication loan = approved(customerId, name, amount, tenure, purpose, score, rate);
        loan.accepted();
        return loan;
    }

    private LoanApplication disbursed(Long customerId, String name, String amount, int tenure, String purpose,
                                      int score, String rate) {
        LoanApplication loan = accepted(customerId, name, amount, tenure, purpose, score, rate);
        loan.disbursed();
        return loan;
    }

    private LoanApplication rejected(Long customerId, String name, String amount, int tenure, String purpose,
                                     int score, String rate) {
        LoanApplication loan = scored(customerId, name, amount, tenure, purpose, score, rate);
        loan.decided(LoanStatus.REJECTED, "Debt-service ratio or credit policy threshold was not met.");
        return loan;
    }
}
