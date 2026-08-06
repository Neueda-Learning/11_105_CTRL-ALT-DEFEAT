package com.finance.PaymentProcessing.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.finance.PaymentProcessing.dto.PaymentHistoryResponse;
import com.finance.PaymentProcessing.exception.NotFoundException;
import com.finance.PaymentProcessing.model.Payment;
import com.finance.PaymentProcessing.model.PaymentHistory;
import com.finance.PaymentProcessing.model.PaymentStatus;
import com.finance.PaymentProcessing.repository.PaymentHistoryRepository;
import com.finance.PaymentProcessing.repository.PaymentRepository;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class HistoryServiceTest {

    @Mock
    private PaymentHistoryRepository historyRepository;

    @Mock
    private PaymentRepository paymentRepository;

    private HistoryService service;

    @BeforeEach
    void setUp() {
        service = new HistoryService(historyRepository, paymentRepository);
    }

    private Payment payment() {
        Payment p = new Payment();
        p.setPaymentId("100000001");
        return p;
    }

    @Test
    void recordTransition_defaultsBlankActorToApiClient() {
        service.recordTransition(payment(), null, PaymentStatus.SENT, "remarks", "ERR", "  ");

        ArgumentCaptor<PaymentHistory> captor = ArgumentCaptor.forClass(PaymentHistory.class);
        verify(historyRepository).save(captor.capture());
        assertEquals("API_CLIENT", captor.getValue().getActor());
        assertEquals(PaymentStatus.SENT, captor.getValue().getNewStatus());
    }

    @Test
    void recordTransition_defaultsNullActorToApiClient() {
        service.recordTransition(payment(), PaymentStatus.CREATED, PaymentStatus.VALIDATED, "remarks", null, null);

        ArgumentCaptor<PaymentHistory> captor = ArgumentCaptor.forClass(PaymentHistory.class);
        verify(historyRepository).save(captor.capture());
        assertEquals("API_CLIENT", captor.getValue().getActor());
    }

    @Test
    void recordTransition_keepsProvidedActor() {
        service.recordTransition(payment(), PaymentStatus.CREATED, PaymentStatus.VALIDATED, "remarks", null, "SYSTEM");

        ArgumentCaptor<PaymentHistory> captor = ArgumentCaptor.forClass(PaymentHistory.class);
        verify(historyRepository).save(captor.capture());
        assertEquals("SYSTEM", captor.getValue().getActor());
    }

    @Test
    void getHistory_paymentNotFound_throwsNotFoundException() {
        when(paymentRepository.existsById("missing")).thenReturn(false);

        assertThrows(NotFoundException.class, () -> service.getHistory("missing"));
    }

    @Test
    void getHistory_paymentFound_returnsMappedHistory() {
        when(paymentRepository.existsById("100000001")).thenReturn(true);
        PaymentHistory history = new PaymentHistory();
        history.setHistoryId("1");
        history.setNewStatus(PaymentStatus.SENT);
        history.setTimestamp(Instant.now());
        history.setActor("SYSTEM");
        when(historyRepository.findByPaymentIdOrderByTimestampAsc("100000001")).thenReturn(List.of(history));

        List<PaymentHistoryResponse> result = service.getHistory("100000001");

        assertEquals(1, result.size());
        assertEquals("1", result.get(0).historyId());
    }
}
