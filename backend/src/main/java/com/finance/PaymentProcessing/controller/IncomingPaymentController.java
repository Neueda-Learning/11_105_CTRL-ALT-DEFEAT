package com.finance.PaymentProcessing.controller;

import com.finance.PaymentProcessing.dto.CurrentUserResponse;
import com.finance.PaymentProcessing.dto.IncomingPaymentRequest;
import com.finance.PaymentProcessing.dto.IncomingPaymentResponse;
import com.finance.PaymentProcessing.util.UniqueIdGenerator;
import jakarta.validation.Valid;
import java.math.BigDecimal;
import java.net.URI;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class IncomingPaymentController {

    private static final String DEFAULT_PAYER_ID = "100000001";
    private static final List<IncomingPaymentResponse> STORE = new CopyOnWriteArrayList<>();

    @GetMapping("/api/users/current")
    public CurrentUserResponse getCurrentUser() {
        return new CurrentUserResponse(DEFAULT_PAYER_ID);
    }

    @GetMapping("/api/incoming-payments")
    public List<IncomingPaymentResponse> listIncomingPayments() {
        return new ArrayList<>(STORE);
    }

    @PostMapping("/api/incoming-payments")
    public ResponseEntity<IncomingPaymentResponse> createIncomingPayment(
        @Valid @RequestBody IncomingPaymentRequest request
    ) {
        Instant now = Instant.now();
        IncomingPaymentResponse created = new IncomingPaymentResponse(
            UniqueIdGenerator.generate(),
            DEFAULT_PAYER_ID,
            sanitizeAmount(request.amount()),
            request.currency().trim(),
            request.reference().trim(),
            request.sourceName().trim(),
            request.destinationAccountId(),
            request.receivedAt() != null ? request.receivedAt() : now,
            now,
            now
        );
        STORE.add(created);
        return ResponseEntity
            .created(URI.create("/api/incoming-payments/" + created.incomingPaymentId()))
            .body(created);
    }

    private BigDecimal sanitizeAmount(BigDecimal amount) {
        return amount.stripTrailingZeros();
    }
}
