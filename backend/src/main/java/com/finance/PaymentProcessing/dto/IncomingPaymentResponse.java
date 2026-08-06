package com.finance.PaymentProcessing.dto;

import java.math.BigDecimal;
import java.time.Instant;

public record IncomingPaymentResponse(
    String incomingPaymentId,
    String payerId,
    BigDecimal amount,
    String currency,
    String reference,
    String sourceName,
    String destinationAccountId,
    Instant receivedAt,
    Instant createdAt,
    Instant updatedAt
) {
}
