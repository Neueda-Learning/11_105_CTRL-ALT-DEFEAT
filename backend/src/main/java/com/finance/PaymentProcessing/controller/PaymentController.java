package com.finance.PaymentProcessing.controller;

import com.finance.PaymentProcessing.dto.*;
import com.finance.PaymentProcessing.model.PaymentStatus;
import com.finance.PaymentProcessing.service.PaymentService;
import jakarta.validation.Valid;
import java.net.URI;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/payments")
public class PaymentController {
    private final PaymentService service;

    public PaymentController(PaymentService service) {
        this.service = service;
    }

    @PostMapping
    public ResponseEntity<PaymentResponse> createPayment(@RequestHeader("Idempotency-Key") String idempotencyKey,
            @Valid @RequestBody PaymentRequest request) {
        PaymentCreationResult result = service.createPayment(request, idempotencyKey);
        if (!result.created()) return ResponseEntity.ok(result.payment());
        return ResponseEntity.created(URI.create("/api/payments/" + result.payment().paymentId())).body(result.payment());
    }

    @GetMapping("/{id}")
    public PaymentResponse getPayment(@PathVariable String id) {
        return service.getPayment(id);
    }

    @PatchMapping("/{id}/status")
    public PaymentResponse updatePaymentStatus(@PathVariable String id,
            @Valid @RequestBody PaymentStatusRequest request) {
        return service.updateStatus(id, request);
    }

    @GetMapping
    public Page<PaymentResponse> listPayments(@RequestParam(required = false) PaymentStatus status,
            @PageableDefault(size = 20, sort = "createdAt") Pageable pageable) {
        return service.listPayments(status, pageable);
    }
}
