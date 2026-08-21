package com.traceflow.loanservice.client;

import java.math.BigDecimal;
import java.util.regex.Pattern;

import com.traceflow.loanservice.model.LoanApplication;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

@Component
public class DisbursementGatewayClient {

    private static final Logger log = LoggerFactory.getLogger(DisbursementGatewayClient.class);
    private static final Pattern CURRENCY_PATTERN = Pattern.compile("^[A-Z]{3}$");

    private final RestClient paymentProvider;
    private final String paymentProviderApiPath;
    private final String defaultCurrency;

    public DisbursementGatewayClient(
            RestClient paymentProvider,
            @Value("${PAYMENT_PROVIDER_API_PATH}") String paymentProviderApiPath,
            @Value("${payment.default-currency:USD}") String defaultCurrency) {
        this.paymentProvider = paymentProvider;
        this.paymentProviderApiPath = paymentProviderApiPath;
        this.defaultCurrency = defaultCurrency;
    }

    public void transferFunds(LoanApplication loan, String transactionId) {
        BigDecimal settlementAmount = calculateSettlementAmount(loan);

        try {
            String currency = resolveCurrency(loan);
            PaymentProviderDisbursementResponse response = paymentProvider.post()
                    .uri(paymentProviderApiPath)
                    .body(new PaymentProviderDisbursementRequest(transactionId, settlementAmount, currency))
                    .retrieve()
                    .body(PaymentProviderDisbursementResponse.class);
            log.info("payment_gateway_request_succeeded loanApplicationId={} transactionId={} providerReference={}",
                    loan.getId(), transactionId, response == null ? "unknown" : response.providerReference());
        } catch (RestClientResponseException ex) {
            if (ex.getStatusCode().value() == 400) {
                log.error("third_party_api_contract_mismatch loanApplicationId={} providerApiPath={} "
                                + "legacyPayloadFields=transactionId,amount,currency providerResponse={}",
                        loan.getId(), paymentProviderApiPath, ex.getResponseBodyAsString(), ex);
                throw new ThirdPartyApiContractException(
                        "Third-party disbursement API rejected the legacy payload; its contract has changed");
            }
            throw ex;
        }
    }

    private String resolveCurrency(LoanApplication loan) {
        String currency = loan == null ? null : loan.getCurrency();
        if (currency != null && CURRENCY_PATTERN.matcher(currency).matches()) {
            return currency;
        }
        currency = defaultCurrency;
        if (currency != null && CURRENCY_PATTERN.matcher(currency).matches()) {
            return currency;
        }
        throw new IllegalArgumentException("Disbursement currency must match ^[A-Z]{3}$");
    }

    private BigDecimal calculateSettlementAmount(LoanApplication loan) {
        return loan.getAmount();
    }
}
