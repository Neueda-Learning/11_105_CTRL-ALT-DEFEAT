package com.finance.PaymentProcessing.dto;

import com.finance.PaymentProcessing.model.PaymentStatus;
import java.time.Instant;

public record PaymentHistoryResponse(String historyId, PaymentStatus oldStatus, PaymentStatus newStatus,
        Instant timestamp, String remarks, String errorCode, String actor) {
}
