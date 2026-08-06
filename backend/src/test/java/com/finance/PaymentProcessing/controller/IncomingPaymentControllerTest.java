package com.finance.PaymentProcessing.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.finance.PaymentProcessing.dto.CurrentUserResponse;
import com.finance.PaymentProcessing.dto.IncomingPaymentRequest;
import com.finance.PaymentProcessing.dto.IncomingPaymentResponse;
import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

class IncomingPaymentControllerTest {

    private final IncomingPaymentController controller = new IncomingPaymentController();

    @BeforeEach
    @SuppressWarnings("unchecked")
    void clearStore() throws Exception {
        Field field = IncomingPaymentController.class.getDeclaredField("STORE");
        field.setAccessible(true);
        ((CopyOnWriteArrayList<IncomingPaymentResponse>) field.get(null)).clear();
    }

    @Test
    void getCurrentUser_returnsDefaultPayerId() {
        CurrentUserResponse response = controller.getCurrentUser();

        assertEquals("100000001", response.payerId());
    }

    @Test
    void listIncomingPayments_emptyByDefault() {
        assertTrue(controller.listIncomingPayments().isEmpty());
    }

    @Test
    void createIncomingPayment_storesAndReturnsCreated() {
        IncomingPaymentRequest request = new IncomingPaymentRequest(
                new BigDecimal("100.500"), " usd ", " ref ", " Acme Corp ", "200000002", null);

        ResponseEntity<IncomingPaymentResponse> response = controller.createIncomingPayment(request);

        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        IncomingPaymentResponse body = response.getBody();
        assertNotNull(body);
        assertEquals("100000001", body.payerId());
        assertEquals(new BigDecimal("100.5"), body.amount());
        assertEquals("usd", body.currency());
        assertEquals("ref", body.reference());
        assertEquals("Acme Corp", body.sourceName());
        assertEquals("200000002", body.destinationAccountId());
        assertNotNull(body.receivedAt());
        assertNotNull(body.createdAt());

        List<IncomingPaymentResponse> stored = controller.listIncomingPayments();
        assertEquals(1, stored.size());
        assertEquals(body.incomingPaymentId(), stored.get(0).incomingPaymentId());
    }

    @Test
    void createIncomingPayment_usesProvidedReceivedAt() {
        Instant receivedAt = Instant.parse("2024-01-01T00:00:00Z");
        IncomingPaymentRequest request = new IncomingPaymentRequest(
                new BigDecimal("50.00"), "INR", "ref-2", "Source Two", null, receivedAt);

        ResponseEntity<IncomingPaymentResponse> response = controller.createIncomingPayment(request);

        assertEquals(receivedAt, response.getBody().receivedAt());
    }
}
