package com.finance.PaymentProcessing.repository.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.finance.PaymentProcessing.model.Beneficiary;
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
class BeneficiaryRepositoryImplTest {

    @Mock
    private JdbcTemplate jdbc;

    private BeneficiaryRepositoryImpl repository;

    @BeforeEach
    void setUp() {
        repository = new BeneficiaryRepositoryImpl(jdbc);
    }

    private Beneficiary beneficiary() {
        Beneficiary b = new Beneficiary();
        b.setName("Alice");
        b.setAccountNumber("ACC123456");
        b.setBankName("Bank");
        b.setIfscCode("ABCD0123456");
        b.setEmail("alice@example.com");
        b.setPhone("9999999999");
        return b;
    }

    @Test
    void save_newBeneficiary_insertsWithGeneratedId() {
        Beneficiary beneficiary = beneficiary();

        Beneficiary saved = repository.save(beneficiary);

        assertNotNull(saved.getBeneficiaryId());
        verify(jdbc).update(anyString(), eq(saved.getBeneficiaryId()), eq("Alice"), eq("ACC123456"), eq("Bank"),
                eq("ABCD0123456"), eq("alice@example.com"), eq("9999999999"));
    }

    @Test
    void save_existingBeneficiary_updatesRow() {
        Beneficiary beneficiary = beneficiary();
        beneficiary.setBeneficiaryId("100000001");

        repository.save(beneficiary);

        verify(jdbc).update(anyString(), eq("Alice"), eq("ACC123456"), eq("Bank"), eq("ABCD0123456"),
                eq("alice@example.com"), eq("9999999999"), eq("100000001"));
    }

    @Test
    void findById_present_mapsRow() throws Exception {
        ResultSet rs = org.mockito.Mockito.mock(ResultSet.class);
        when(rs.getString("beneficiary_id")).thenReturn("100000001");
        when(rs.getString("name")).thenReturn("Alice");
        when(rs.getString("account_number")).thenReturn("ACC123456");
        when(rs.getString("bank_name")).thenReturn("Bank");
        when(rs.getString("ifsc_code")).thenReturn("ABCD0123456");
        when(rs.getString("email")).thenReturn("alice@example.com");
        when(rs.getString("phone")).thenReturn("9999999999");

        ArgumentCaptor<RowMapper<Beneficiary>> captor = ArgumentCaptor.forClass(RowMapper.class);
        when(jdbc.query(anyString(), captor.capture(), eq("100000001"))).thenAnswer(inv ->
                List.of(captor.getValue().mapRow(rs, 0)));

        Optional<Beneficiary> result = repository.findById("100000001");

        assertTrue(result.isPresent());
        assertEquals("Alice", result.get().getName());
    }

    @Test
    void findById_absent_returnsEmpty() {
        when(jdbc.query(anyString(), org.mockito.ArgumentMatchers.<RowMapper<Beneficiary>>any(), eq("missing")))
                .thenReturn(List.of());

        assertFalse(repository.findById("missing").isPresent());
    }

    @Test
    void findAll_returnsAllRows() {
        when(jdbc.query(anyString(), org.mockito.ArgumentMatchers.<RowMapper<Beneficiary>>any()))
                .thenReturn(List.of(beneficiary(), beneficiary()));

        assertEquals(2, repository.findAll().size());
    }

    @Test
    void existsById_true() {
        when(jdbc.queryForObject(anyString(), eq(Integer.class), eq("100000001"))).thenReturn(1);

        assertTrue(repository.existsById("100000001"));
    }

    @Test
    void existsById_false() {
        when(jdbc.queryForObject(anyString(), eq(Integer.class), eq("missing"))).thenReturn(0);

        assertFalse(repository.existsById("missing"));
    }

    @Test
    void findByAccountNumber_present() {
        when(jdbc.query(anyString(), org.mockito.ArgumentMatchers.<RowMapper<Beneficiary>>any(), eq("ACC123456")))
                .thenReturn(List.of(beneficiary()));

        Optional<Beneficiary> result = repository.findByAccountNumber("ACC123456");

        assertTrue(result.isPresent());
    }

    @Test
    void findByAccountNumber_absent() {
        when(jdbc.query(anyString(), org.mockito.ArgumentMatchers.<RowMapper<Beneficiary>>any(), eq("missing")))
                .thenReturn(List.of());

        assertFalse(repository.findByAccountNumber("missing").isPresent());
    }
}
