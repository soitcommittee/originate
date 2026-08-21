package com.traceflow.loanservice.client;

import com.traceflow.loanservice.domain.LoanApplication;
import com.traceflow.loanservice.exception.PaymentGatewayTimeoutException;
import com.traceflow.loanservice.exception.ThirdPartyApiContractException;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.client.MockRestServiceServer;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withBadRequest;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;
import static org.springframework.http.MediaType.APPLICATION_JSON;

class DisbursementGatewayClientTests {
    private final LoanApplication loan = new LoanApplication(
            1L, "Test Applicant", new BigDecimal("50000"), 60, "Home renovation");

    @Test
    void transferSucceedsWhenDemoFailureIsDisabled() {
        var builder = org.springframework.web.client.RestClient.builder();
        var server = MockRestServiceServer.bindTo(builder).build();
        server.expect(requestTo("http://provider.test/v1/disbursements"))
                .andRespond(withSuccess("{\"providerReference\":\"PG-test\",\"apiVersion\":\"v1\",\"processedAt\":\"2026-01-01T00:00:00Z\"}", APPLICATION_JSON));
        DisbursementGatewayClient client = new DisbursementGatewayClient(builder, false, 3000, false,
                "http://provider.test", "/v1/disbursements");
        assertDoesNotThrow(() -> client.transferFunds(loan));
        server.verify();
    }

    @Test
    void transferPreservesNestedTimeoutCauseWhenDemoFailureIsEnabled() {
        DisbursementGatewayClient client = new DisbursementGatewayClient(org.springframework.web.client.RestClient.builder(), true,
                3000, false, "http://provider.test", "/v1/disbursements");
        PaymentGatewayTimeoutException error = assertThrows(
                PaymentGatewayTimeoutException.class, () -> client.transferFunds(loan));
        assertInstanceOf(java.net.SocketTimeoutException.class, error.getCause());
    }

    @Test
    void transferFailsWithBigDecimalConversionErrorWhenDemoFlagIsEnabled() {
        DisbursementGatewayClient client = new DisbursementGatewayClient(org.springframework.web.client.RestClient.builder(), false,
                3000, true, "http://provider.test", "/v1/disbursements");

        assertThrows(ArithmeticException.class, () -> client.transferFunds(loan));
    }

    @Test
    void transferFailsWhenProviderUpgradesItsApiContract() {
        var builder = org.springframework.web.client.RestClient.builder();
        var server = MockRestServiceServer.bindTo(builder).build();
        server.expect(requestTo("http://provider.test/v2/disbursements"))
                .andRespond(withBadRequest().body("{\"code\":\"REQUIRED_FIELD_MISSING\",\"requiredField\":\"currency\"}"));
        DisbursementGatewayClient client = new DisbursementGatewayClient(builder, false, 3000, false,
                "http://provider.test", "/v2/disbursements");

        assertThrows(ThirdPartyApiContractException.class, () -> client.transferFunds(loan));
        server.verify();
    }
}
