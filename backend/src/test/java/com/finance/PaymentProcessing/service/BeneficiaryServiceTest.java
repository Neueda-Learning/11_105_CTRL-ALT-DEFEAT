package com.finance.PaymentProcessing.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.finance.PaymentProcessing.dto.BeneficiaryRequest;
import com.finance.PaymentProcessing.dto.BeneficiaryResponse;
import com.finance.PaymentProcessing.exception.BadRequestException;
import com.finance.PaymentProcessing.exception.NotFoundException;
import com.finance.PaymentProcessing.model.Beneficiary;
import com.finance.PaymentProcessing.repository.BeneficiaryRepository;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class BeneficiaryServiceTest {

    @Mock
    private BeneficiaryRepository repository;

    private BeneficiaryService service;

    @BeforeEach
    void setUp() {
        service = new BeneficiaryService(repository);
    }

    private Beneficiary beneficiary(String id) {
        Beneficiary b = new Beneficiary();
        b.setBeneficiaryId(id);
        b.setName("Alice");
        b.setAccountNumber("ACC123456");
        b.setBankName("Bank");
        b.setIfscCode("ABCD0123456");
        b.setEmail("alice@example.com");
        b.setPhone("9999999999");
        return b;
    }

    @Test
    void addBeneficiary_savesNormalizedBeneficiary() {
        BeneficiaryRequest request = new BeneficiaryRequest("Alice", "ACC123456", "Bank", "abcd0123456",
                "alice@example.com", "9999999999");
        when(repository.findByAccountNumber("ACC123456")).thenReturn(Optional.empty());
        when(repository.save(any(Beneficiary.class))).thenAnswer(inv -> {
            Beneficiary b = inv.getArgument(0);
            b.setBeneficiaryId("100000001");
            return b;
        });

        BeneficiaryResponse response = service.addBeneficiary(request);

        ArgumentCaptor<Beneficiary> captor = ArgumentCaptor.forClass(Beneficiary.class);
        verify(repository).save(captor.capture());
        assertEquals("ABCD0123456", captor.getValue().getIfscCode());
        assertEquals("100000001", response.beneficiaryId());
        assertEquals("Alice", response.name());
    }

    @Test
    void addBeneficiary_duplicateAccountNumber_throwsBadRequest() {
        BeneficiaryRequest request = new BeneficiaryRequest("Alice", "ACC123456", "Bank", "ABCD0123456",
                "alice@example.com", "9999999999");
        when(repository.findByAccountNumber("ACC123456")).thenReturn(Optional.of(beneficiary("100000001")));

        assertThrows(BadRequestException.class, () -> service.addBeneficiary(request));
        verify(repository, never()).save(any());
    }

    @Test
    void listBeneficiaries_mapsAllRecords() {
        when(repository.findAll()).thenReturn(List.of(beneficiary("1"), beneficiary("2")));

        List<BeneficiaryResponse> result = service.listBeneficiaries();

        assertEquals(2, result.size());
    }

    @Test
    void getBeneficiary_found_returnsResponse() {
        when(repository.findById("100000001")).thenReturn(Optional.of(beneficiary("100000001")));

        BeneficiaryResponse response = service.getBeneficiary("100000001");

        assertEquals("100000001", response.beneficiaryId());
    }

    @Test
    void getBeneficiary_notFound_throwsNotFoundException() {
        when(repository.findById("missing")).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> service.getBeneficiary("missing"));
    }
}
