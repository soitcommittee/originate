package com.traceflow.decisionservice.repositories;

import com.traceflow.decisionservice.domain.CreditPolicy;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;

@Repository
public class InMemoryCreditPolicyRepository implements CreditPolicyRepository {
    private static final CreditPolicy ACTIVE_POLICY = new CreditPolicy(
            new BigDecimal("0.50"),
            new BigDecimal("1.00"),
            760,
            690,
            610,
            new BigDecimal("2.50"),
            new BigDecimal("5.25"),
            650
    );

    @Override
    public CreditPolicy activePolicy() {
        return ACTIVE_POLICY;
    }
}
