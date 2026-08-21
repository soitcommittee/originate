package com.traceflow.loanservice.client;

import com.traceflow.loanservice.exception.ThirdPartyApiContractException;
import com.traceflow.loanservice.model.LoanApplication;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import java.math.BigDecimal;

@Component
public class DisbursementGatewayClient {

    private static final Logger log = LoggerFactory.getLogger(DisbursementGatewayClient.class);

    private final RestClient paymentProvider;
    private final String paymentProviderApiPath;

    public DisbursementGatewayClient(RestClient paymentProvider,
                                     @Value("${PAYMENT_PROVIDER_API_PATH}") String paymentProviderApiPath) {
        this.paymentProvider = paymentProvider;
        this.paymentProviderApiPath = paymentProviderApiPath;
    }

    public void transferFunds(LoanApplication loan, BigDecimal settlementAmount, String transactionId) {
        try {
            PaymentProviderDisbursementResponse response = paymentProvider.post()
                    .uri(paymentProviderApiPath)
                    .body(new PaymentProviderDisbursementRequest(transactionId, settlementAmount, loan.getCurrency()))
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
