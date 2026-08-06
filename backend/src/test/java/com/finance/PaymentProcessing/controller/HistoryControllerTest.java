package com.finance.PaymentProcessing.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

import com.finance.PaymentProcessing.dto.PaymentHistoryResponse;
import com.finance.PaymentProcessing.model.PaymentStatus;
import com.finance.PaymentProcessing.service.HistoryService;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class HistoryControllerTest {

    @Mock
    private HistoryService service;

    private HistoryController controller;

    @BeforeEach
    void setUp() {
        controller = new HistoryController(service);
    }

    @Test
    void getPaymentHistory_delegatesToService() {
        PaymentHistoryResponse response = new PaymentHistoryResponse("1", null, PaymentStatus.SENT,
                Instant.now(), "remarks", null, "SYSTEM");
        when(service.getHistory("100000001")).thenReturn(List.of(response));

        List<PaymentHistoryResponse> result = controller.getPaymentHistory("100000001");

        assertEquals(1, result.size());
        assertEquals(response, result.get(0));
    }

    @Test
    void getTransactionTimeline_delegatesToService() {
        PaymentHistoryResponse response = new PaymentHistoryResponse("1", null, PaymentStatus.SENT,
                Instant.now(), "remarks", null, "SYSTEM");
        when(service.getHistory("100000001")).thenReturn(List.of(response));

        List<PaymentHistoryResponse> result = controller.getTransactionTimeline("100000001");

        assertEquals(1, result.size());
        assertEquals(response, result.get(0));
    }
}
