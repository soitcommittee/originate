package com.traceflow.paymentprovider;

import com.traceflow.paymentprovider.dto.DisbursementRequest;
import com.traceflow.paymentprovider.dto.DisbursementResponse;
import com.traceflow.paymentprovider.dto.ProviderErrorResponse;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.regex.Pattern;

@RestController
public class PaymentProviderController {
    private static final Logger log = LoggerFactory.getLogger(PaymentProviderController.class);
    private static final Pattern CURRENCY = Pattern.compile("^[A-Z]{3}$");

    @PostMapping("/v1/disbursements")
    public DisbursementResponse disburseV1(@Valid @RequestBody DisbursementRequest request) {
        return accepted(request, "v1");
    }

    @PostMapping("/v2/disbursements")
    public ResponseEntity<?> disburseV2(@Valid @RequestBody DisbursementRequest request) {
        if (request.currency() == null || !CURRENCY.matcher(request.currency()).matches()) {
            log.warn("provider_contract_rejection apiVersion=v2 transactionId={} requiredField=currency "
                            + "reason=missing_or_invalid_format",
                    request.transactionId());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new ProviderErrorResponse(
                    Instant.now(), 400, "REQUIRED_FIELD_MISSING", "currency is required and must be a 3-letter uppercase code", "currency"));
        }
        return ResponseEntity.ok(accepted(request, "v2"));
    }

    @PostMapping("/v3/disbursements")
    public ResponseEntity<?> disburseV3(@Valid @RequestBody DisbursementRequest request) {
        String providerReference = "PG-" + request.transactionId() + "-" + System.currentTimeMillis();
        String changedTimestamp = LocalDateTime.now(ZoneOffset.UTC)
                .format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"));
        log.warn("provider_response_contract_change apiVersion=v3 transactionId={} changedField=processedAt",
                request.transactionId());
        return ResponseEntity.ok(Map.of(
                "providerReference", providerReference,
                "apiVersion", "v3",
                "processedAt", changedTimestamp));
    }

    private DisbursementResponse accepted(DisbursementRequest request, String version) {
        String providerReference = "PG-" + request.transactionId() + "-" + System.currentTimeMillis();
        log.info("provider_disbursement_accepted apiVersion={} transactionId={} providerReference={}",
                version, request.transactionId(), providerReference);
        return new DisbursementResponse(providerReference, version, Instant.now());
    }
}
