package com.finance.PaymentProcessing.repository.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.finance.PaymentProcessing.model.BankAccount;
import java.sql.ResultSet;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;

@ExtendWith(MockitoExtension.class)
class BankAccountRepositoryImplTest {

    @Mock
    private JdbcTemplate jdbc;

    private BankAccountRepositoryImpl repository;

    @BeforeEach
    void setUp() {
        repository = new BankAccountRepositoryImpl(jdbc);
    }

    private BankAccount account(String number, String holder) {
        BankAccount a = new BankAccount();
        a.setAccountNumber(number);
        a.setAccountHolderName(holder);
        a.setActive(true);
        return a;
    }

    @Test
    void save_newAccount_insertsWithGeneratedId() {
        BankAccount account = account("ACC123456", "Alice");

        BankAccount saved = repository.save(account);

        assertNotNull(saved.getAccountId());
        assertEquals(9, saved.getAccountId().length());
        verify(jdbc).update(anyString(), eq(saved.getAccountId()), eq("ACC123456"), eq("Alice"), eq(true));
    }

    @Test
    void save_existingAccount_updatesRow() {
        BankAccount account = account("ACC123456", "Alice");
        account.setAccountId("100000001");

        repository.save(account);

        verify(jdbc).update(anyString(), eq("ACC123456"), eq("Alice"), eq(true), eq("100000001"));
    }

    @Test
    void findById_present_mapsRowToAccount() throws Exception {
        ResultSet rs = org.mockito.Mockito.mock(ResultSet.class);
        when(rs.getString("account_id")).thenReturn("100000001");
        when(rs.getString("account_number")).thenReturn("ACC123456");
        when(rs.getString("account_holder_name")).thenReturn("Alice");
        when(rs.getBoolean("active")).thenReturn(true);

        ArgumentCaptor<RowMapper<BankAccount>> captor = ArgumentCaptor.forClass(RowMapper.class);
        when(jdbc.query(anyString(), captor.capture(), eq("100000001"))).thenAnswer(inv -> {
            RowMapper<BankAccount> mapper = captor.getValue();
            return List.of(mapper.mapRow(rs, 0));
        });

        Optional<BankAccount> result = repository.findById("100000001");

        assertTrue(result.isPresent());
        assertEquals("ACC123456", result.get().getAccountNumber());
        assertEquals("Alice", result.get().getAccountHolderName());
        assertTrue(result.get().isActive());
    }

    @Test
    void findById_absent_returnsEmpty() {
        when(jdbc.query(anyString(), org.mockito.ArgumentMatchers.<RowMapper<BankAccount>>any(), eq("missing")))
                .thenReturn(List.of());

        Optional<BankAccount> result = repository.findById("missing");

        assertFalse(result.isPresent());
    }

    @Test
    void findAll_returnsAllRows() {
        when(jdbc.query(anyString(), org.mockito.ArgumentMatchers.<RowMapper<BankAccount>>any()))
                .thenReturn(List.of(account("ACC1", "A"), account("ACC2", "B")));

        List<BankAccount> result = repository.findAll();

        assertEquals(2, result.size());
    }

    @Test
    void existsById_true_whenCountPositive() {
        when(jdbc.queryForObject(anyString(), eq(Integer.class), eq("100000001"))).thenReturn(1);

        assertTrue(repository.existsById("100000001"));
    }

    @Test
    void existsById_false_whenCountZero() {
        when(jdbc.queryForObject(anyString(), eq(Integer.class), eq("missing"))).thenReturn(0);

        assertFalse(repository.existsById("missing"));
    }
}
