package com.finance.PaymentProcessing.dto;

import com.finance.PaymentProcessing.model.CardType;
import com.finance.PaymentProcessing.model.PaymentMethod;
import com.finance.PaymentProcessing.model.PaymentStatus;
import com.finance.PaymentProcessing.model.PaymentType;
import java.math.BigDecimal;
import java.time.Instant;

public record PaymentResponse(String paymentId, BigDecimal amount, String currency, String reference,
        PaymentStatus status, PaymentType paymentType, PaymentMethod paymentMethod, CardType cardType,
        String payerId, String invoiceId, String sourceAccountId, String beneficiaryId,
        String cardLast4, String cardHolderName, String upiId,
        Instant createdAt, Instant updatedAt) {
}
