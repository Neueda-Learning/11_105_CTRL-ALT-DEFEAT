package com.finance.PaymentProcessing.service;

import com.finance.PaymentProcessing.dto.PaymentRequest;
import com.finance.PaymentProcessing.exception.BadRequestException;
import com.finance.PaymentProcessing.exception.ConflictException;
import com.finance.PaymentProcessing.exception.NotFoundException;
import com.finance.PaymentProcessing.model.Beneficiary;
import com.finance.PaymentProcessing.model.Payment;
import com.finance.PaymentProcessing.model.PaymentStatus;
import com.finance.PaymentProcessing.model.PaymentType;
import com.finance.PaymentProcessing.repository.BeneficiaryRepository;
import com.finance.PaymentProcessing.repository.PaymentRepository;
import com.finance.PaymentProcessing.util.UniqueIdGenerator;
import java.math.BigDecimal;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PaymentFailureAuditService {

    private final PaymentRepository paymentRepository;
    private final BeneficiaryRepository beneficiaryRepository;
    private final HistoryService historyService;

    public PaymentFailureAuditService(
            PaymentRepository paymentRepository,
            BeneficiaryRepository beneficiaryRepository,
            HistoryService historyService) {
        this.paymentRepository = paymentRepository;
        this.beneficiaryRepository = beneficiaryRepository;
        this.historyService = historyService;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void persistFailedAttempt(PaymentRequest request, String idempotencyKey, RuntimeException ex) {
        String beneficiaryId = resolveBeneficiaryId(request.beneficiaryId());
        if (beneficiaryId == null) {
            return;
        }

        try {
            Payment failed = new Payment();
            failed.setAmount(sanitizeAmount(request.amount()));
            failed.setCurrency(sanitizeCurrency(request.currency()));
            failed.setReference(sanitizeReference(request.reference()));
            failed.setSourceAccountId(null);
            failed.setBeneficiaryId(beneficiaryId);
            failed.setPayerId(request.payerId());
            failed.setPaymentType(request.paymentType() != null ? request.paymentType() : PaymentType.BILL_PAYMENT);
            failed.setInvoiceId(null);
            failed.setIdempotencyKey(idempotencyKey + "-FAILED-" + UniqueIdGenerator.generate());
            failed.setStatus(PaymentStatus.FAILED);

            Payment saved = paymentRepository.save(failed);
            historyService.recordTransition(saved, null, PaymentStatus.FAILED,
                    ex.getMessage(), resolveErrorCode(ex), "SYSTEM");
        } catch (RuntimeException ignored) {
            // Keep original exception behavior if audit persistence cannot be recorded.
        }
    }

    private String resolveBeneficiaryId(String requestedId) {
        if (requestedId != null && beneficiaryRepository.existsById(requestedId)) {
            return requestedId;
        }
        return beneficiaryRepository.findAll().stream()
                .map(Beneficiary::getBeneficiaryId)
                .findFirst()
                .orElse(null);
    }

    private BigDecimal sanitizeAmount(BigDecimal amount) {
        if (amount == null || amount.signum() <= 0) {
            return BigDecimal.ONE;
        }
        return amount;
    }

    private String sanitizeCurrency(String currency) {
        if (currency == null || currency.isBlank()) {
            return "INR";
        }
        String normalized = currency.trim().toUpperCase();
        if (normalized.length() >= 3) {
            return normalized.substring(0, 3);
        }
        return (normalized + "INR").substring(0, 3);
    }

    private String sanitizeReference(String reference) {
        if (reference == null || reference.isBlank()) {
            return "FAILED_VALIDATION";
        }
        return reference.length() > 255 ? reference.substring(0, 255) : reference;
    }

    private String resolveErrorCode(RuntimeException ex) {
        if (ex instanceof BadRequestException badRequestException) {
            return badRequestException.getErrorCode();
        }
        if (ex instanceof ConflictException conflictException) {
            return conflictException.getErrorCode();
        }
        if (ex instanceof NotFoundException notFoundException) {
            return notFoundException.getErrorCode();
        }
        return "VALIDATION_FAILED";
    }
}
