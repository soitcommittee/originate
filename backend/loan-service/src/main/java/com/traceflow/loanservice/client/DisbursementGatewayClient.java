package com.traceflow.loanservice.client;

import com.traceflow.loanservice.exception.ThirdPartyApiContractException;
import com.traceflow.loanservice.model.LoanApplication;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.math.BigDecimal;

@Component
public class DisbursementGatewayClient {

    private static final Logger log = LoggerFactory.getLogger(DisbursementGatewayClient.class);

    private final WebClient paymentProvider;

    @Value("${payment.provider.api-path}")
    private String paymentProviderApiPath;

    @Value("${loan.default-currency:USD}")
    private String defaultCurrency;

    public DisbursementGatewayClient(WebClient paymentProvider) {
        this.paymentProvider = paymentProvider;
    }

    public PaymentProviderDisbursementResponse transferFunds(String transactionId, BigDecimal settlementAmount, LoanApplication loan) {
        String currency = resolveCurrency(loan);
        try {
            return paymentProvider.post()
                    .uri(paymentProviderApiPath)
                    .body(new PaymentProviderDisbursementRequest(transactionId, settlementAmount, currency))
                    .retrieve()
                    .body(PaymentProviderDisbursementResponse.class);
        } catch (WebClientResponseException e) {
            log.error("third_party_api_contract_mismatch: payment provider rejected disbursement for transaction {} with status {}: {}",
                    transactionId, e.getStatusCode(), e.getResponseBodyAsString(), e);
            throw new ThirdPartyApiContractException("Payment provider rejected disbursement: " + e.getResponseBodyAsString());
        }
    }

    private String resolveCurrency(LoanApplication loan) {
        if (loan != null && loan.getCurrency() != null && !loan.getCurrency().isBlank()) {
            return loan.getCurrency().trim().toUpperCase();
        }
        return defaultCurrency;
    }
}
