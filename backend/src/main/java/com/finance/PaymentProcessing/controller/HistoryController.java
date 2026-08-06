package com.finance.PaymentProcessing.controller;

import com.finance.PaymentProcessing.dto.PaymentHistoryResponse;
import com.finance.PaymentProcessing.service.HistoryService;
import java.util.List;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/payments/{paymentId}/history")
public class HistoryController {
    private final HistoryService service;

    public HistoryController(HistoryService service) {
        this.service = service;
    }

    @GetMapping
    public List<PaymentHistoryResponse> getPaymentHistory(@PathVariable String paymentId) {
        return service.getHistory(paymentId);
    }

    @GetMapping("/timeline")
    public List<PaymentHistoryResponse> getTransactionTimeline(@PathVariable String paymentId) {
        return service.getHistory(paymentId);
    }
}
