package com.traceflow.decisionservice.repositories;

import com.traceflow.decisionservice.domain.CreditPolicy;

public interface CreditPolicyRepository {
    CreditPolicy activePolicy();
}
