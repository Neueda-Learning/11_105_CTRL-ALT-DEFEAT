package com.finance.PaymentProcessing.service;

import com.finance.PaymentProcessing.dto.PaymentHistoryResponse;
import com.finance.PaymentProcessing.exception.NotFoundException;
import com.finance.PaymentProcessing.model.*;
import com.finance.PaymentProcessing.repository.PaymentHistoryRepository;
import com.finance.PaymentProcessing.repository.PaymentRepository;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class HistoryService {
    private final PaymentHistoryRepository historyRepository;
    private final PaymentRepository paymentRepository;

    public HistoryService(PaymentHistoryRepository historyRepository, PaymentRepository paymentRepository) {
        this.historyRepository = historyRepository;
        this.paymentRepository = paymentRepository;
    }

    public void recordTransition(Payment payment, PaymentStatus oldStatus, PaymentStatus newStatus, String remarks,
            String errorCode, String actor) {
        PaymentHistory history = new PaymentHistory();
        history.setPaymentId(payment.getPaymentId());
        history.setOldStatus(oldStatus);
        history.setNewStatus(newStatus);
        history.setRemarks(remarks);
        history.setErrorCode(errorCode);
        history.setActor(actor == null || actor.isBlank() ? "API_CLIENT" : actor);
        historyRepository.save(history);
    }

    @Transactional(readOnly = true)
    public List<PaymentHistoryResponse> getHistory(String paymentId) {
        if (!paymentRepository.existsById(paymentId))
            throw new NotFoundException("Payment not found: " + paymentId);
        return historyRepository.findByPaymentIdOrderByTimestampAsc(paymentId).stream()
                .map(h -> new PaymentHistoryResponse(h.getHistoryId(), h.getOldStatus(), h.getNewStatus(),
                        h.getTimestamp(), h.getRemarks(), h.getErrorCode(), h.getActor()))
                .toList();
    }
}