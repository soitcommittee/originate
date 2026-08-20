package com.traceflow.decisionservice.services.impl;

import com.traceflow.decisionservice.domain.CreditPolicy;
import com.traceflow.decisionservice.domain.DecisionRecommendation;
import com.traceflow.decisionservice.dto.requests.DecisionEvaluationRequest;
import com.traceflow.decisionservice.dto.responses.DecisionEvaluationResponse;
import com.traceflow.decisionservice.repositories.CreditPolicyRepository;
import com.traceflow.decisionservice.services.DecisionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;

@Service
public class DecisionServiceImpl implements DecisionService {
    private static final Logger log = LoggerFactory.getLogger(DecisionServiceImpl.class);
    private final CreditPolicyRepository policyRepository;
    private final boolean demoDecisionErrorEnabled;

    public DecisionServiceImpl(
            CreditPolicyRepository policyRepository,
            @Value("${demo.decision.error.enabled:false}") boolean demoDecisionErrorEnabled) {
        this.policyRepository = policyRepository;
        this.demoDecisionErrorEnabled = demoDecisionErrorEnabled;
    }

    @Override
    public DecisionEvaluationResponse evaluate(DecisionEvaluationRequest request) {
        if (demoDecisionErrorEnabled) {
            log.error("demo_decision_service_error customerId={} productCode={} reason=simulated_outage",
                    request.customerId(), request.productCode());
            throw new IllegalStateException("Simulated decision service outage");
        }

        CreditPolicy policy = policyRepository.activePolicy();
        BigDecimal annualIncome = request.monthlyIncome().multiply(BigDecimal.valueOf(12));
        BigDecimal exposureRatio = request.requestedAmount().divide(annualIncome, 4, RoundingMode.HALF_UP);

        int score;
        BigDecimal premium;
        if (exposureRatio.compareTo(policy.lowRiskRatio()) <= 0) {
            score = policy.lowRiskScore();
            premium = BigDecimal.ZERO;
        } else if (exposureRatio.compareTo(policy.mediumRiskRatio()) <= 0) {
            score = policy.mediumRiskScore();
            premium = policy.mediumRiskPremium();
        } else {
            score = policy.highRiskScore();
            premium = policy.highRiskPremium();
        }

        boolean amountAllowed = request.requestedAmount().compareTo(request.minAmount()) >= 0
                && request.requestedAmount().compareTo(request.maxAmount()) <= 0;
        boolean tenureAllowed = request.tenureMonths() >= request.minTenureMonths()
                && request.tenureMonths() <= request.maxTenureMonths();

        DecisionRecommendation recommendation;
        String reason;
        if (!amountAllowed) {
            recommendation = DecisionRecommendation.DECLINE;
            reason = "Requested amount is outside product limits";
        } else if (!tenureAllowed) {
            recommendation = DecisionRecommendation.DECLINE;
            reason = "Requested tenure is outside product limits";
        } else if (score >= policy.automaticEligibilityScore()) {
            recommendation = DecisionRecommendation.ELIGIBLE;
            reason = "Affordability and product policy checks passed";
        } else {
            recommendation = DecisionRecommendation.REFER;
            reason = "Manual underwriting review required for elevated exposure";
        }

        BigDecimal interestRate = request.baseInterestRate().add(premium).setScale(2, RoundingMode.HALF_UP);
        log.info("credit_decision_evaluated customerId={} productCode={} exposureRatio={} creditScore={} recommendation={} interestRate={}",
                request.customerId(), request.productCode(), exposureRatio, score, recommendation, interestRate);

        return new DecisionEvaluationResponse(request.customerId(), request.productCode(), score, interestRate,
                recommendation, reason, Instant.now());
    }
}
