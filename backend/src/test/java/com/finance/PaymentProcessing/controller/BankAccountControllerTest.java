package com.finance.PaymentProcessing.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.finance.PaymentProcessing.dto.BankAccountRequest;
import com.finance.PaymentProcessing.dto.BankAccountResponse;
import com.finance.PaymentProcessing.exception.BadRequestException;
import com.finance.PaymentProcessing.model.BankAccount;
import com.finance.PaymentProcessing.repository.BankAccountRepository;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

@ExtendWith(MockitoExtension.class)
class BankAccountControllerTest {

    @Mock
    private BankAccountRepository repository;

    private BankAccountController controller;

    @BeforeEach
    void setUp() {
        controller = new BankAccountController(repository);
    }

    private BankAccount account(String id, String number, String holder, boolean active) {
        BankAccount a = new BankAccount();
        a.setAccountId(id);
        a.setAccountNumber(number);
        a.setAccountHolderName(holder);
        a.setActive(active);
        return a;
    }

    @Test
    void create_savesNewAccount_andReturnsCreated() {
        when(repository.findAll()).thenReturn(List.of());
        when(repository.save(any(BankAccount.class))).thenAnswer(inv -> {
            BankAccount a = inv.getArgument(0);
            a.setAccountId("100000001");
            return a;
        });

        ResponseEntity<BankAccountResponse> response =
                controller.create(new BankAccountRequest("ACC123456", "John Doe"));

        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        assertEquals("100000001", response.getBody().accountId());
        assertEquals("ACC123456", response.getBody().accountNumber());
        assertTrue(response.getBody().active());
        verify(repository).save(any(BankAccount.class));
    }

    @Test
    void create_duplicateAccountNumber_throwsBadRequest() {
        when(repository.findAll()).thenReturn(List.of(account("1", "ACC123456", "Jane", true)));

        assertThrows(BadRequestException.class,
                () -> controller.create(new BankAccountRequest("ACC123456", "John Doe")));
    }

    @Test
    void list_returnsMappedAccounts() {
        when(repository.findAll()).thenReturn(List.of(
                account("1", "ACC111111", "Alice", true),
                account("2", "ACC222222", "Bob", false)));

        List<BankAccountResponse> result = controller.list();

        assertEquals(2, result.size());
        assertEquals("ACC111111", result.get(0).accountNumber());
        assertTrue(result.get(0).active());
        assertEquals("ACC222222", result.get(1).accountNumber());
        assertTrue(!result.get(1).active());
    }
}
