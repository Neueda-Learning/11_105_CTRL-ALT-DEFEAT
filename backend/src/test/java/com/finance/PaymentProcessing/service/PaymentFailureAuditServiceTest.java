package com.finance.PaymentProcessing.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.finance.PaymentProcessing.dto.PaymentRequest;
import com.finance.PaymentProcessing.exception.BadRequestException;
import com.finance.PaymentProcessing.exception.ConflictException;
import com.finance.PaymentProcessing.exception.NotFoundException;
import com.finance.PaymentProcessing.model.Beneficiary;
import com.finance.PaymentProcessing.model.Payment;
import com.finance.PaymentProcessing.model.PaymentMethod;
import com.finance.PaymentProcessing.model.PaymentType;
import com.finance.PaymentProcessing.repository.BeneficiaryRepository;
import com.finance.PaymentProcessing.repository.PaymentRepository;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class PaymentFailureAuditServiceTest {

    @Mock
    private PaymentRepository paymentRepository;

    @Mock
    private BeneficiaryRepository beneficiaryRepository;

    @Mock
    private HistoryService historyService;

    private PaymentFailureAuditService service;

    @BeforeEach
    void setUp() {
        service = new PaymentFailureAuditService(paymentRepository, beneficiaryRepository, historyService);
    }

    private PaymentRequest request(BigDecimal amount, String currency, String reference, String beneficiaryId) {
        return new PaymentRequest(amount, currency, reference, "payer", PaymentMethod.NET_BANKING, beneficiaryId,
                null, null, null, null, null, null, null, null, null);
    }

    private Beneficiary beneficiary(String id) {
        Beneficiary b = new Beneficiary();
        b.setBeneficiaryId(id);
        return b;
    }

    @Test
    void persistFailedAttempt_noBeneficiaryAvailable_doesNothing() {
        when(beneficiaryRepository.existsById(any())).thenReturn(false);
        when(beneficiaryRepository.findAll()).thenReturn(List.of());

        service.persistFailedAttempt(request(BigDecimal.TEN, "INR", "ref", "unknown"),
                "key-1", new BadRequestException("VALIDATION_FAILED", "bad"));

        verify(paymentRepository, never()).save(any());
    }

    @Test
    void persistFailedAttempt_usesRequestedBeneficiaryWhenExists() {
        when(beneficiaryRepository.existsById("100000001")).thenReturn(true);
        when(paymentRepository.save(any(Payment.class))).thenAnswer(inv -> inv.getArgument(0));

        service.persistFailedAttempt(request(BigDecimal.TEN, "INR", "ref", "100000001"),
                "key-1", new BadRequestException("INVALID_CARD", "bad card"));

        ArgumentCaptor<Payment> captor = ArgumentCaptor.forClass(Payment.class);
        verify(paymentRepository).save(captor.capture());
        assertEquals("100000001", captor.getValue().getBeneficiaryId());
        verify(historyService).recordTransition(any(), any(), any(), any(), org.mockito.ArgumentMatchers.eq("INVALID_CARD"),
                org.mockito.ArgumentMatchers.eq("SYSTEM"));
    }

    @Test
    void persistFailedAttempt_fallsBackToFirstBeneficiary_whenRequestedMissing() {
        when(beneficiaryRepository.existsById("missing")).thenReturn(false);
        when(beneficiaryRepository.findAll()).thenReturn(List.of(beneficiary("200000002")));
        when(paymentRepository.save(any(Payment.class))).thenAnswer(inv -> inv.getArgument(0));

        service.persistFailedAttempt(request(BigDecimal.TEN, "INR", "ref", "missing"),
                "key-1", new NotFoundException("not found"));

        ArgumentCaptor<Payment> captor = ArgumentCaptor.forClass(Payment.class);
        verify(paymentRepository).save(captor.capture());
        assertEquals("200000002", captor.getValue().getBeneficiaryId());
    }

    @Test
    void persistFailedAttempt_nullBeneficiaryId_fallsBackToFirstBeneficiary() {
        when(beneficiaryRepository.findAll()).thenReturn(List.of(beneficiary("300000003")));
        when(paymentRepository.save(any(Payment.class))).thenAnswer(inv -> inv.getArgument(0));

        service.persistFailedAttempt(request(BigDecimal.TEN, "INR", "ref", null),
                "key-1", new ConflictException("DUPLICATE_PAYMENT", "dup"));

        ArgumentCaptor<Payment> captor = ArgumentCaptor.forClass(Payment.class);
        verify(paymentRepository).save(captor.capture());
        assertEquals("300000003", captor.getValue().getBeneficiaryId());
    }

    @Test
    void persistFailedAttempt_sanitizesNonPositiveAmountToOne() {
        when(beneficiaryRepository.existsById("100000001")).thenReturn(true);
        when(paymentRepository.save(any(Payment.class))).thenAnswer(inv -> inv.getArgument(0));

        service.persistFailedAttempt(request(BigDecimal.ZERO, "INR", "ref", "100000001"),
                "key-1", new BadRequestException("bad"));

        ArgumentCaptor<Payment> captor = ArgumentCaptor.forClass(Payment.class);
        verify(paymentRepository).save(captor.capture());
        assertEquals(BigDecimal.ONE, captor.getValue().getAmount());
    }

    @Test
    void persistFailedAttempt_sanitizesBlankCurrencyToInr() {
        when(beneficiaryRepository.existsById("100000001")).thenReturn(true);
        when(paymentRepository.save(any(Payment.class))).thenAnswer(inv -> inv.getArgument(0));

        service.persistFailedAttempt(request(BigDecimal.TEN, "  ", "ref", "100000001"),
                "key-1", new BadRequestException("bad"));

        ArgumentCaptor<Payment> captor = ArgumentCaptor.forClass(Payment.class);
        verify(paymentRepository).save(captor.capture());
        assertEquals("INR", captor.getValue().getCurrency());
    }

    @Test
    void persistFailedAttempt_padsShortCurrencyWithInr() {
        when(beneficiaryRepository.existsById("100000001")).thenReturn(true);
        when(paymentRepository.save(any(Payment.class))).thenAnswer(inv -> inv.getArgument(0));

        service.persistFailedAttempt(request(BigDecimal.TEN, "u", "ref", "100000001"),
                "key-1", new BadRequestException("bad"));

        ArgumentCaptor<Payment> captor = ArgumentCaptor.forClass(Payment.class);
        verify(paymentRepository).save(captor.capture());
        assertEquals("UIN", captor.getValue().getCurrency());
    }

    @Test
    void persistFailedAttempt_sanitizesBlankReference() {
        when(beneficiaryRepository.existsById("100000001")).thenReturn(true);
        when(paymentRepository.save(any(Payment.class))).thenAnswer(inv -> inv.getArgument(0));

        service.persistFailedAttempt(request(BigDecimal.TEN, "INR", "  ", "100000001"),
                "key-1", new BadRequestException("bad"));

        ArgumentCaptor<Payment> captor = ArgumentCaptor.forClass(Payment.class);
        verify(paymentRepository).save(captor.capture());
        assertEquals("FAILED_VALIDATION", captor.getValue().getReference());
    }

    @Test
    void persistFailedAttempt_truncatesLongReference() {
        when(beneficiaryRepository.existsById("100000001")).thenReturn(true);
        when(paymentRepository.save(any(Payment.class))).thenAnswer(inv -> inv.getArgument(0));
        String longReference = "a".repeat(300);

        service.persistFailedAttempt(request(BigDecimal.TEN, "INR", longReference, "100000001"),
                "key-1", new BadRequestException("bad"));

        ArgumentCaptor<Payment> captor = ArgumentCaptor.forClass(Payment.class);
        verify(paymentRepository).save(captor.capture());
        assertEquals(255, captor.getValue().getReference().length());
    }

    @Test
    void persistFailedAttempt_genericRuntimeException_usesValidationFailedCode() {
        when(beneficiaryRepository.existsById("100000001")).thenReturn(true);
        when(paymentRepository.save(any(Payment.class))).thenAnswer(inv -> inv.getArgument(0));

        service.persistFailedAttempt(request(BigDecimal.TEN, "INR", "ref", "100000001"),
                "key-1", new IllegalStateException("boom"));

        verify(historyService).recordTransition(any(), any(), any(), any(),
                org.mockito.ArgumentMatchers.eq("VALIDATION_FAILED"), org.mockito.ArgumentMatchers.eq("SYSTEM"));
    }

    @Test
    void persistFailedAttempt_repositorySaveThrows_isSwallowed() {
        when(beneficiaryRepository.existsById("100000001")).thenReturn(true);
        when(paymentRepository.save(any(Payment.class))).thenThrow(new RuntimeException("db down"));

        service.persistFailedAttempt(request(BigDecimal.TEN, "INR", "ref", "100000001"),
                "key-1", new BadRequestException("bad"));

        verify(historyService, never()).recordTransition(any(), any(), any(), any(), any(), any());
    }
}
