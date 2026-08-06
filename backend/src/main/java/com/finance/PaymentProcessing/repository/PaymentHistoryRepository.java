package com.finance.PaymentProcessing.repository;

import com.finance.PaymentProcessing.model.PaymentHistory;
import java.util.List;

public interface PaymentHistoryRepository {
    void save(PaymentHistory history);
    List<PaymentHistory> findByPaymentIdOrderByTimestampAsc(String paymentId);
}
