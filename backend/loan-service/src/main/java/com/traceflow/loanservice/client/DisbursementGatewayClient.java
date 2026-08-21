package com.traceflow.loanservice.client;

import com.traceflow.loanservice.domain.LoanApplication;
import com.traceflow.loanservice.exception.PaymentGatewayTimeoutException;
import com.traceflow.loanservice.exception.ThirdPartyApiContractException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.SocketTimeoutException;

@Component
public class DisbursementGatewayClient {
    private static final Logger log = LoggerFactory.getLogger(DisbursementGatewayClient.class);

    private final boolean demoErrorEnabled;
    private final boolean demoBigDecimalErrorEnabled;
    private final RestClient paymentProvider;
    private final String paymentProviderApiPath;
    private final long simulatedTimeoutMs;

    public DisbursementGatewayClient(
            RestClient.Builder restClientBuilder,
            @Value("${demo.error.enabled:false}") boolean demoErrorEnabled,
            @Value("${demo.error.simulated-timeout-ms:3000}") long simulatedTimeoutMs,
            @Value("${demo.bigdecimal.error.enabled:false}") boolean demoBigDecimalErrorEnabled,
            @Value("${payment.provider.url:http://localhost:8090}") String paymentProviderUrl,
            @Value("${payment.provider.api-path:/v1/disbursements}") String paymentProviderApiPath) {
        this.demoErrorEnabled = demoErrorEnabled;
        this.simulatedTimeoutMs = simulatedTimeoutMs;
        this.demoBigDecimalErrorEnabled = demoBigDecimalErrorEnabled;
        this.paymentProvider = restClientBuilder.baseUrl(paymentProviderUrl).build();
        this.paymentProviderApiPath = paymentProviderApiPath;
    }

    public void transferFunds(LoanApplication loan) {
        String transactionId = "DISB-" + loan.getId() + "-01";
        log.info("payment_gateway_request_started loanApplicationId={} transactionId={} amount={}",
                loan.getId(), transactionId, loan.getRequestedAmount());

        if (demoBigDecimalErrorEnabled) {
            BigDecimal amountWithCents = loan.getRequestedAmount()
                    .setScale(0, RoundingMode.DOWN)
                    .add(new BigDecimal("0.50"));
            log.warn("demo_bigdecimal_conversion_error loanApplicationId={} amount={}",
                    loan.getId(), amountWithCents);
            amountWithCents.intValueExact();
        }

        if (demoErrorEnabled) {
            log.warn("payment_gateway_request_retry loanApplicationId={} transactionId={} attempt=2 reason=read_timeout",
                    loan.getId(), transactionId);
            SocketTimeoutException rootCause = new SocketTimeoutException(
                    "Read timed out calling POST /v1/disbursements after " + simulatedTimeoutMs + "ms");
            throw new PaymentGatewayTimeoutException(
                    "Payment gateway failed after 2 attempts for transaction " + transactionId, rootCause);
        }

        try {
            PaymentProviderDisbursementResponse response = paymentProvider.post()
                    .uri(paymentProviderApiPath)
                    .body(new PaymentProviderDisbursementRequest(transactionId, loan.getRequestedAmount()))
                    .retrieve()
                    .body(PaymentProviderDisbursementResponse.class);
            log.info("payment_gateway_request_succeeded loanApplicationId={} transactionId={} providerReference={}",
                    loan.getId(), transactionId, response == null ? "unknown" : response.providerReference());
        } catch (RestClientResponseException ex) {
            if (ex.getStatusCode().value() == 400) {
                log.error("third_party_api_contract_mismatch loanApplicationId={} providerApiPath={} "
                                + "legacyPayloadFields=transactionId,amount providerResponse={}",
                        loan.getId(), paymentProviderApiPath, ex.getResponseBodyAsString(), ex);
                throw new ThirdPartyApiContractException(
                        "Third-party disbursement API rejected the legacy payload; its contract has changed");
            }
            throw ex;
        }
    }
}
