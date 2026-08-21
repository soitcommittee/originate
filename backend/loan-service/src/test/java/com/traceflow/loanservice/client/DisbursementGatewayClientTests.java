package com.traceflow.loanservice.client;

import com.traceflow.loanservice.domain.LoanApplication;
import com.traceflow.loanservice.exception.PaymentGatewayTimeoutException;
import com.traceflow.loanservice.exception.ThirdPartyApiContractException;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;

class DisbursementGatewayClientTests {
    private final LoanApplication loan = new LoanApplication(
            1L, "Test Applicant", new BigDecimal("50000"), 60, "Home renovation");

    @Test
    void transferSucceedsWhenDemoFailureIsDisabled() {
        DisbursementGatewayClient client = new DisbursementGatewayClient(false, 3000, false, false);
        assertDoesNotThrow(() -> client.transferFunds(loan));
    }

    @Test
    void transferPreservesNestedTimeoutCauseWhenDemoFailureIsEnabled() {
        DisbursementGatewayClient client = new DisbursementGatewayClient(true, 3000, false, false);
        PaymentGatewayTimeoutException error = assertThrows(
                PaymentGatewayTimeoutException.class, () -> client.transferFunds(loan));
        assertInstanceOf(java.net.SocketTimeoutException.class, error.getCause());
    }

    @Test
    void transferFailsWithBigDecimalConversionErrorWhenDemoFlagIsEnabled() {
        DisbursementGatewayClient client = new DisbursementGatewayClient(false, 3000, true, false);

        assertThrows(ArithmeticException.class, () -> client.transferFunds(loan));
    }

    @Test
    void transferFailsWhenProviderUpgradesItsApiContract() {
        DisbursementGatewayClient client = new DisbursementGatewayClient(false, 3000, false, true);

        assertThrows(ThirdPartyApiContractException.class, () -> client.transferFunds(loan));
    }
}
