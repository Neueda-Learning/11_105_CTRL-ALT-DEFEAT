package com.finance.PaymentProcessing.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import com.finance.PaymentProcessing.dto.PaymentCreationResult;
import com.finance.PaymentProcessing.dto.PaymentRequest;
import com.finance.PaymentProcessing.dto.PaymentResponse;
import com.finance.PaymentProcessing.dto.PaymentStatusRequest;
import com.finance.PaymentProcessing.model.PaymentMethod;
import com.finance.PaymentProcessing.model.PaymentStatus;
import com.finance.PaymentProcessing.model.PaymentType;
import com.finance.PaymentProcessing.service.PaymentService;
import java.math.BigDecimal;
import java.time.Instant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

@ExtendWith(MockitoExtension.class)
class PaymentControllerTest {

    @Mock
    private PaymentService service;

    private PaymentController controller;

    @BeforeEach
    void setUp() {
        controller = new PaymentController(service);
    }

    private PaymentResponse response(String id) {
        return new PaymentResponse(id, new BigDecimal("10.00"), "INR", "ref", PaymentStatus.SENT,
                PaymentType.BENEFICIARY_TRANSFER, PaymentMethod.NET_BANKING, null, "payer", null, null,
                "beneficiary", null, null, null, Instant.now(), Instant.now());
    }

    @Test
    void createPayment_newPayment_returnsCreated() {
        PaymentRequest request = new PaymentRequest(new BigDecimal("10.00"), "INR", "ref", "payer",
                PaymentMethod.NET_BANKING, "beneficiary", null, null, null, null, null, null, null, null, null);
        PaymentResponse response = response("100000001");
        when(service.createPayment(request, "key-1")).thenReturn(new PaymentCreationResult(response, true));

        ResponseEntity<PaymentResponse> result = controller.createPayment("key-1", request);

        assertEquals(HttpStatus.CREATED, result.getStatusCode());
        assertEquals(response, result.getBody());
    }

    @Test
    void createPayment_idempotentReplay_returnsOk() {
        PaymentRequest request = new PaymentRequest(new BigDecimal("10.00"), "INR", "ref", "payer",
                PaymentMethod.NET_BANKING, "beneficiary", null, null, null, null, null, null, null, null, null);
        PaymentResponse response = response("100000001");
        when(service.createPayment(request, "key-1")).thenReturn(new PaymentCreationResult(response, false));

        ResponseEntity<PaymentResponse> result = controller.createPayment("key-1", request);

        assertEquals(HttpStatus.OK, result.getStatusCode());
        assertEquals(response, result.getBody());
    }

    @Test
    void getPayment_delegatesToService() {
        PaymentResponse response = response("100000001");
        when(service.getPayment("100000001")).thenReturn(response);

        assertEquals(response, controller.getPayment("100000001"));
    }

    @Test
    void updatePaymentStatus_delegatesToService() {
        PaymentStatusRequest request = new PaymentStatusRequest(PaymentStatus.COMPLETED, "done", null, "SYSTEM");
        PaymentResponse response = response("100000001");
        when(service.updateStatus("100000001", request)).thenReturn(response);

        assertEquals(response, controller.updatePaymentStatus("100000001", request));
    }

    @Test
    void listPayments_withStatus_delegatesToService() {
        Pageable pageable = PageRequest.of(0, 20);
        Page<PaymentResponse> page = new PageImpl<>(java.util.List.of(response("100000001")));
        when(service.listPayments(eq(PaymentStatus.SENT), any(Pageable.class))).thenReturn(page);

        Page<PaymentResponse> result = controller.listPayments(PaymentStatus.SENT, pageable);

        assertEquals(page, result);
    }

    @Test
    void listPayments_withoutStatus_delegatesToService() {
        Pageable pageable = PageRequest.of(0, 20);
        Page<PaymentResponse> page = new PageImpl<>(java.util.List.of(response("100000001")));
        when(service.listPayments(eq(null), any(Pageable.class))).thenReturn(page);

        Page<PaymentResponse> result = controller.listPayments(null, pageable);

        assertEquals(page, result);
    }
}
