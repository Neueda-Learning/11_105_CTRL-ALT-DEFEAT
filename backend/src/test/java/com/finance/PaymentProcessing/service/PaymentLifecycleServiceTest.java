package com.finance.PaymentProcessing.service;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.finance.PaymentProcessing.model.Payment;
import com.finance.PaymentProcessing.model.PaymentStatus;
import com.finance.PaymentProcessing.repository.PaymentRepository;
import java.lang.reflect.Method;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.transaction.PlatformTransactionManager;

@ExtendWith(MockitoExtension.class)
class PaymentLifecycleServiceTest {

    @Mock
    private PaymentRepository paymentRepository;

    @Mock
    private HistoryService historyService;

    @Mock
    private PlatformTransactionManager transactionManager;

    private PaymentLifecycleService service;

    @BeforeEach
    void setUp() {
        service = new PaymentLifecycleService(paymentRepository, historyService, transactionManager);
    }

    private Payment payment(PaymentStatus status) {
        Payment p = new Payment();
        p.setPaymentId("100000001");
        p.setStatus(status);
        return p;
    }

    private void invokeCompleteIfSent(String paymentId) throws Exception {
        Method method = PaymentLifecycleService.class.getDeclaredMethod("completeIfSent", String.class);
        method.setAccessible(true);
        method.invoke(service, paymentId);
    }

    @Test
    void completeIfSent_transitionsSentPaymentToCompleted() throws Exception {
        Payment sentPayment = payment(PaymentStatus.SENT);
        when(paymentRepository.findById("100000001")).thenReturn(Optional.of(sentPayment));
        when(paymentRepository.save(any(Payment.class))).thenAnswer(inv -> inv.getArgument(0));

        invokeCompleteIfSent("100000001");

        assertEquals(PaymentStatus.COMPLETED, sentPayment.getStatus());
        verify(paymentRepository).save(sentPayment);
        verify(historyService).recordTransition(sentPayment, PaymentStatus.SENT, PaymentStatus.COMPLETED,
                "Auto-completed after 30 seconds", null, "SYSTEM");
    }

    @Test
    void completeIfSent_ignoresPaymentsNotInSentStatus() throws Exception {
        Payment completedPayment = payment(PaymentStatus.COMPLETED);
        when(paymentRepository.findById("100000001")).thenReturn(Optional.of(completedPayment));

        invokeCompleteIfSent("100000001");

        verify(paymentRepository, never()).save(any());
        verify(historyService, never()).recordTransition(any(), any(), any(), any(), any(), any());
    }

    @Test
    void completeIfSent_missingPayment_doesNothing() throws Exception {
        when(paymentRepository.findById("missing")).thenReturn(Optional.empty());

        invokeCompleteIfSent("missing");

        verify(paymentRepository, never()).save(any());
    }

    @Test
    void scheduleCompletion_doesNotThrow() {
        assertDoesNotThrow(() -> service.scheduleCompletion("100000001"));
    }

    @Test
    void shutdown_stopsSchedulerWithoutException() {
        assertDoesNotThrow(service::shutdown);
    }
}
