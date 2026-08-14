package com.traceflow.loanservice.client;

import com.traceflow.loanservice.domain.LoanApplication;
import com.traceflow.loanservice.exception.PaymentGatewayTimeoutException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.net.SocketTimeoutException;

@Component
public class DisbursementGatewayClient {
    private static final Logger log = LoggerFactory.getLogger(DisbursementGatewayClient.class);

    private final boolean demoErrorEnabled;
    private final long simulatedTimeoutMs;

    public DisbursementGatewayClient(
            @Value("${demo.error.enabled:false}") boolean demoErrorEnabled,
            @Value("${demo.error.simulated-timeout-ms:3000}") long simulatedTimeoutMs) {
        this.demoErrorEnabled = demoErrorEnabled;
        this.simulatedTimeoutMs = simulatedTimeoutMs;
    }

    public void transferFunds(LoanApplication loan) {
        String transactionId = "DISB-" + loan.getId() + "-01";
        log.info("payment_gateway_request_started loanApplicationId={} transactionId={} amount={}",
                loan.getId(), transactionId, loan.getRequestedAmount());

        if (demoErrorEnabled) {
            log.warn("payment_gateway_request_retry loanApplicationId={} transactionId={} attempt=2 reason=read_timeout",
                    loan.getId(), transactionId);
            SocketTimeoutException rootCause = new SocketTimeoutException(
                    "Read timed out calling POST /v1/disbursements after " + simulatedTimeoutMs + "ms");
            throw new PaymentGatewayTimeoutException(
                    "Payment gateway failed after 2 attempts for transaction " + transactionId, rootCause);
        }

        log.info("payment_gateway_request_succeeded loanApplicationId={} transactionId={} providerReference=PG-{}",
                loan.getId(), transactionId, System.currentTimeMillis());
    }
}

