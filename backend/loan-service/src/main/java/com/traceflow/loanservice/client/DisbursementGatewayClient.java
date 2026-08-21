package com.traceflow.loanservice.client;

import com.traceflow.loanservice.domain.Loan;
import com.traceflow.loanservice.exception.ThirdPartyApiContractException;
import java.math.BigDecimal;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.reactive.function.client.WebClient;

@Component
public class DisbursementGatewayClient {

    private static final Logger log = LoggerFactory.getLogger(DisbursementGatewayClient.class);
    private static final String DEFAULT_CURRENCY = "SGD";

    private final WebClient paymentProvider;
    private final String paymentProviderApiPath;

    public DisbursementGatewayClient(WebClient.Builder webClientBuilder,
                                     @Value("${payment.provider.base-url}") String paymentProviderBaseUrl,
                                     @Value("${payment.provider.api-path}") String paymentProviderApiPath) {
        this.paymentProvider = webClientBuilder.baseUrl(paymentProviderBaseUrl).build();
        this.paymentProviderApiPath = paymentProviderApiPath;
    }

    public PaymentProviderDisbursementResponse transferFunds(Loan loan, BigDecimal settlementAmount) {
        String transactionId = UUID.randomUUID().toString();
        PaymentProviderDisbursementRequest request = new PaymentProviderDisbursementRequest(transactionId, settlementAmount);
        request.setCurrency(DEFAULT_CURRENCY);
        try {
            PaymentProviderDisbursementResponse response = paymentProvider.post()
                    .uri(paymentProviderApiPath)
                    .body(request)
                    .retrieve()
                    .body(PaymentProviderDisbursementResponse.class);
            log.info("payment_gateway_request_succeeded loanApplicationId={} transactionId={} providerReference={}",
                    loan.getId(), transactionId, response == null ? "unknown" : response.providerReference());
            return response;
        } catch (RestClientResponseException ex) {
            if (ex.getStatusCode().value() == 400) {
                log.error("third_party_api_contract_mismatch loanApplicationId={} providerApiPath={} "
                                + "payloadFields=transactionId,amount,currency providerResponse={}",
                        loan.getId(), paymentProviderApiPath, ex.getResponseBodyAsString(), ex);
                throw new ThirdPartyApiContractException(
                        "Third-party disbursement API rejected the payload; its contract may have changed");
            }
            throw ex;
        }
    }
}
