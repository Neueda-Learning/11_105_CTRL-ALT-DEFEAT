package com.finance.PaymentProcessing.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.finance.PaymentProcessing.dto.BeneficiaryRequest;
import com.finance.PaymentProcessing.dto.BeneficiaryResponse;
import com.finance.PaymentProcessing.service.BeneficiaryService;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

@ExtendWith(MockitoExtension.class)
class BeneficiaryControllerTest {

    @Mock
    private BeneficiaryService service;

    private BeneficiaryController controller;

    @BeforeEach
    void setUp() {
        controller = new BeneficiaryController(service);
    }

    @Test
    void addBeneficiary_delegatesToServiceAndReturnsCreated() {
        BeneficiaryRequest request = new BeneficiaryRequest("Alice", "ACC123456", "Bank", "ABCD0123456",
                "alice@example.com", "9999999999");
        BeneficiaryResponse response = new BeneficiaryResponse("100000001", "Alice", "ACC123456", "Bank",
                "ABCD0123456", "alice@example.com", "9999999999");
        when(service.addBeneficiary(request)).thenReturn(response);

        ResponseEntity<BeneficiaryResponse> result = controller.addBeneficiary(request);

        assertEquals(HttpStatus.CREATED, result.getStatusCode());
        assertEquals(response, result.getBody());
        verify(service).addBeneficiary(request);
    }

    @Test
    void listBeneficiaries_delegatesToService() {
        BeneficiaryResponse response = new BeneficiaryResponse("100000001", "Alice", "ACC123456", "Bank",
                "ABCD0123456", "alice@example.com", "9999999999");
        when(service.listBeneficiaries()).thenReturn(List.of(response));

        List<BeneficiaryResponse> result = controller.listBeneficiaries();

        assertEquals(1, result.size());
        assertEquals(response, result.get(0));
    }

    @Test
    void getBeneficiary_delegatesToService() {
        BeneficiaryResponse response = new BeneficiaryResponse("100000001", "Alice", "ACC123456", "Bank",
                "ABCD0123456", "alice@example.com", "9999999999");
        when(service.getBeneficiary("100000001")).thenReturn(response);

        BeneficiaryResponse result = controller.getBeneficiary("100000001");

        assertEquals(response, result);
    }
}
