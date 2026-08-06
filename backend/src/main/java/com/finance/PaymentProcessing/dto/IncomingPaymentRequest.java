package com.finance.PaymentProcessing.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.Instant;

public record IncomingPaymentRequest(
    @NotNull @DecimalMin(value = "0.01") @Digits(integer = 12, fraction = 2) BigDecimal amount,
    @NotBlank String currency,
    @NotBlank String reference,
    @NotBlank String sourceName,
    String destinationAccountId,
    Instant receivedAt
) {
}
