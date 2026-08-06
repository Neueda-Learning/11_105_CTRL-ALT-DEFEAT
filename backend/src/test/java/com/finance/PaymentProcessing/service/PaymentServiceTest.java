package com.finance.PaymentProcessing.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.finance.PaymentProcessing.dto.PaymentCreationResult;
import com.finance.PaymentProcessing.dto.PaymentRequest;
import com.finance.PaymentProcessing.dto.PaymentResponse;
import com.finance.PaymentProcessing.dto.PaymentStatusRequest;
import com.finance.PaymentProcessing.exception.BadRequestException;
import com.finance.PaymentProcessing.exception.ConflictException;
import com.finance.PaymentProcessing.exception.NotFoundException;
import com.finance.PaymentProcessing.model.CardType;
import com.finance.PaymentProcessing.model.Payment;
import com.finance.PaymentProcessing.model.PaymentMethod;
import com.finance.PaymentProcessing.model.PaymentStatus;
import com.finance.PaymentProcessing.model.PaymentType;
import com.finance.PaymentProcessing.repository.PaymentRepository;
import java.math.BigDecimal;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

@ExtendWith(MockitoExtension.class)
class PaymentServiceTest {

    @Mock
    private PaymentRepository paymentRepository;

    @Mock
    private ValidationService validationService;

    @Mock
    private HistoryService historyService;

    @Mock
    private PaymentLifecycleService paymentLifecycleService;

    @Mock
    private PaymentFailureAuditService paymentFailureAuditService;

    private PaymentService service;

    @BeforeEach
    void setUp() {
        service = new PaymentService(paymentRepository, validationService, historyService,
                paymentLifecycleService, paymentFailureAuditService);
    }

    private PaymentRequest netBankingRequest(BigDecimal amount, String payerId, String beneficiaryId,
            PaymentType type, String invoiceId) {
        return new PaymentRequest(amount, "INR", null, payerId, PaymentMethod.NET_BANKING, beneficiaryId,
                null, null, null, null, null, null, null, type, invoiceId);
    }

    private Payment savedPayment() {
        Payment p = new Payment();
        p.setPaymentId("100000001");
        p.setAmount(BigDecimal.TEN);
        p.setCurrency("INR");
        p.setStatus(PaymentStatus.SENT);
        p.setPaymentType(PaymentType.BENEFICIARY_TRANSFER);
        p.setPaymentMethod(PaymentMethod.NET_BANKING);
        return p;
    }

    @Test
    void createPayment_existingIdempotencyKey_returnsExistingWithoutCreatingFlag() {
        Payment existing = savedPayment();
        when(paymentRepository.findByIdempotencyKey("key-1")).thenReturn(Optional.of(existing));

        PaymentCreationResult result = service.createPayment(
                netBankingRequest(BigDecimal.TEN, "payer", "ben", null, null), "key-1");

        assertFalse(result.created());
        assertEquals("100000001", result.payment().paymentId());
        verify(validationService, never()).validateAmount(any());
    }

    @Test
    void createPayment_netBankingWithoutPayerId_throwsAndAudits() {
        when(paymentRepository.findByIdempotencyKey("key-1")).thenReturn(Optional.empty());

        PaymentRequest request = netBankingRequest(BigDecimal.TEN, null, "ben", null, null);

        assertThrows(BadRequestException.class, () -> service.createPayment(request, "key-1"));
        verify(paymentFailureAuditService).persistFailedAttempt(eq(request), eq("key-1"), any(BadRequestException.class));
        verify(paymentRepository, never()).save(any());
    }

    @Test
    void createPayment_netBankingValid_savesAndSchedulesCompletion() {
        when(paymentRepository.findByIdempotencyKey("key-1")).thenReturn(Optional.empty());
        when(paymentRepository.save(any(Payment.class))).thenAnswer(inv -> {
            Payment p = inv.getArgument(0);
            p.setPaymentId("100000001");
            return p;
        });

        PaymentRequest request = netBankingRequest(BigDecimal.TEN, "payer", "ben", null, null);

        PaymentCreationResult result = service.createPayment(request, "key-1");

        assertTrue(result.created());
        assertEquals("100000001", result.payment().paymentId());
        verify(historyService).recordTransition(any(), eq(null), eq(PaymentStatus.SENT), anyString(), eq(null), eq("SYSTEM"));
        verify(paymentLifecycleService).scheduleCompletion("100000001");
    }

    @Test
    void createPayment_cardPayment_normalizesCardNumberAndSetsLast4() {
        when(paymentRepository.findByIdempotencyKey("key-1")).thenReturn(Optional.empty());
        when(validationService.normalizeCardNumber("4111 1111 1111 1111")).thenReturn("4111111111111111");
        when(paymentRepository.save(any(Payment.class))).thenAnswer(inv -> {
            Payment p = inv.getArgument(0);
            p.setPaymentId("100000002");
            return p;
        });

        PaymentRequest request = new PaymentRequest(BigDecimal.TEN, "INR", null, "payer", PaymentMethod.CARD,
                null, CardType.CREDIT_CARD, "John Doe", "4111 1111 1111 1111", "12", "2030", "123", null, null, null);

        PaymentCreationResult result = service.createPayment(request, "key-1");

        assertTrue(result.created());
        assertEquals("1111", result.payment().cardLast4());
        assertEquals("John Doe", result.payment().cardHolderName());
    }

    @Test
    void createPayment_upiPayment_normalizesUpiId() {
        when(paymentRepository.findByIdempotencyKey("key-1")).thenReturn(Optional.empty());
        when(paymentRepository.save(any(Payment.class))).thenAnswer(inv -> {
            Payment p = inv.getArgument(0);
            p.setPaymentId("100000003");
            return p;
        });

        PaymentRequest request = new PaymentRequest(BigDecimal.TEN, "INR", null, "payer", PaymentMethod.UPI,
                null, null, null, null, null, null, null, "User@UPI", null, null);

        PaymentCreationResult result = service.createPayment(request, "key-1");

        assertEquals("user@upi", result.payment().upiId());
    }

    @Test
    void createPayment_duplicateInvoice_throwsConflictAndAudits() {
        when(paymentRepository.findByIdempotencyKey("key-1")).thenReturn(Optional.empty());
        when(paymentRepository.findByPayerIdAndInvoiceId("payer", "INV-1"))
                .thenReturn(Optional.of(savedPayment()));

        PaymentRequest request = netBankingRequest(BigDecimal.TEN, "payer", "ben", PaymentType.BILL_PAYMENT, "INV-1");

        assertThrows(ConflictException.class, () -> service.createPayment(request, "key-1"));
        verify(paymentFailureAuditService).persistFailedAttempt(eq(request), eq("key-1"), any(ConflictException.class));
    }

    @Test
    void createPayment_invalidAmount_propagatesAndAudits() {
        when(paymentRepository.findByIdempotencyKey("key-1")).thenReturn(Optional.empty());
        doThrow(new BadRequestException("Amount must be greater than zero"))
                .when(validationService).validateAmount(any());

        PaymentRequest request = netBankingRequest(BigDecimal.TEN, "payer", "ben", null, null);

        assertThrows(BadRequestException.class, () -> service.createPayment(request, "key-1"));
        verify(paymentFailureAuditService).persistFailedAttempt(eq(request), eq("key-1"), any(BadRequestException.class));
        verify(paymentRepository, never()).save(any());
    }

    @Test
    void getPayment_found_returnsResponse() {
        when(paymentRepository.findById("100000001")).thenReturn(Optional.of(savedPayment()));

        PaymentResponse response = service.getPayment("100000001");

        assertEquals("100000001", response.paymentId());
    }

    @Test
    void getPayment_notFound_throwsNotFoundException() {
        when(paymentRepository.findById("missing")).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> service.getPayment("missing"));
    }

    @Test
    void updateStatus_valid_transitionsAndRecordsHistory() {
        Payment payment = savedPayment();
        payment.setStatus(PaymentStatus.SENT);
        when(paymentRepository.findById("100000001")).thenReturn(Optional.of(payment));
        when(paymentRepository.save(any(Payment.class))).thenAnswer(inv -> inv.getArgument(0));

        PaymentStatusRequest request = new PaymentStatusRequest(PaymentStatus.COMPLETED, "done", null, "SYSTEM");
        PaymentResponse response = service.updateStatus("100000001", request);

        assertEquals(PaymentStatus.COMPLETED, response.status());
        verify(validationService).validateStatusTransition(PaymentStatus.SENT, PaymentStatus.COMPLETED);
        verify(historyService).recordTransition(payment, PaymentStatus.SENT, PaymentStatus.COMPLETED, "done", null, "SYSTEM");
    }

    @Test
    void listPayments_withStatus_delegatesToRepositoryFindByStatus() {
        Pageable pageable = PageRequest.of(0, 20);
        Page<Payment> page = new PageImpl<>(java.util.List.of(savedPayment()));
        when(paymentRepository.findByStatus(PaymentStatus.SENT, pageable)).thenReturn(page);

        Page<PaymentResponse> result = service.listPayments(PaymentStatus.SENT, pageable);

        assertEquals(1, result.getTotalElements());
        verify(paymentRepository, never()).findAll(any(Pageable.class));
    }

    @Test
    void listPayments_withoutStatus_delegatesToRepositoryFindAll() {
        Pageable pageable = PageRequest.of(0, 20);
        Page<Payment> page = new PageImpl<>(java.util.List.of(savedPayment()));
        when(paymentRepository.findAll(pageable)).thenReturn(page);

        Page<PaymentResponse> result = service.listPayments(null, pageable);

        assertEquals(1, result.getTotalElements());
        verify(paymentRepository, never()).findByStatus(any(), any());
    }
}
