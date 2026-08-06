package com.finance.PaymentProcessing.repository.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.finance.PaymentProcessing.exception.ConflictException;
import com.finance.PaymentProcessing.model.Payment;
import com.finance.PaymentProcessing.model.PaymentMethod;
import com.finance.PaymentProcessing.model.PaymentStatus;
import com.finance.PaymentProcessing.model.PaymentType;
import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;

@ExtendWith(MockitoExtension.class)
class PaymentRepositoryImplTest {

    @Mock
    private JdbcTemplate jdbc;

    private PaymentRepositoryImpl repository;

    @BeforeEach
    void setUp() {
        repository = new PaymentRepositoryImpl(jdbc);
    }

    private Payment payment() {
        Payment p = new Payment();
        p.setAmount(BigDecimal.TEN);
        p.setCurrency("INR");
        p.setReference("ref");
        p.setStatus(PaymentStatus.SENT);
        p.setPaymentType(PaymentType.BENEFICIARY_TRANSFER);
        p.setPaymentMethod(PaymentMethod.NET_BANKING);
        p.setBeneficiaryId("ben");
        return p;
    }

    @Test
    void save_newPayment_insertsWithGeneratedIdAndVersionZero() {
        Payment payment = payment();

        Payment saved = repository.save(payment);

        assertEquals(9, saved.getPaymentId().length());
        assertEquals(0L, saved.getVersion());
        verify(jdbc).update(anyString(), eq(saved.getPaymentId()), eq(BigDecimal.TEN), eq("INR"), eq("ref"),
                eq("SENT"), eq(0L), eq("BENEFICIARY_TRANSFER"), eq("NET_BANKING"), isNull(), isNull(),
                isNull(), isNull(), eq("ben"), isNull(), isNull(), isNull(), isNull(),
                any(), any());
    }

    @Test
    void save_existingPayment_updatesWithIncrementedVersion() {
        Payment payment = payment();
        payment.setPaymentId("100000001");
        payment.setVersion(2L);
        when(jdbc.update(anyString(),
                any(), any(), any(), any(), eq(3L), any(), any(), any(), any(), any(), any(), any(),
                any(), any(), any(), any(), any(), eq("100000001"), eq(2L))).thenReturn(1);

        Payment saved = repository.save(payment);

        assertEquals(3L, saved.getVersion());
    }

    @Test
    void save_existingPayment_optimisticLockFailure_throwsConflict() {
        Payment payment = payment();
        payment.setPaymentId("100000001");
        payment.setVersion(2L);
        when(jdbc.update(anyString(),
                any(), any(), any(), any(), eq(3L), any(), any(), any(), any(), any(), any(), any(),
                any(), any(), any(), any(), any(), eq("100000001"), eq(2L))).thenReturn(0);

        assertThrows(ConflictException.class, () -> repository.save(payment));
    }

    @Test
    void findById_present_mapsRow() throws Exception {
        ResultSet rs = mockPaymentRow();

        ArgumentCaptor<RowMapper<Payment>> captor = ArgumentCaptor.forClass(RowMapper.class);
        when(jdbc.query(anyString(), captor.capture(), eq("100000001"))).thenAnswer(inv ->
                List.of(captor.getValue().mapRow(rs, 0)));

        Optional<Payment> result = repository.findById("100000001");

        assertTrue(result.isPresent());
        assertEquals("100000001", result.get().getPaymentId());
        assertEquals(PaymentStatus.SENT, result.get().getStatus());
    }

    private ResultSet mockPaymentRow() throws Exception {
        ResultSet rs = org.mockito.Mockito.mock(ResultSet.class);
        when(rs.getString("payment_id")).thenReturn("100000001");
        when(rs.getBigDecimal("amount")).thenReturn(BigDecimal.TEN);
        when(rs.getString("currency")).thenReturn("INR");
        when(rs.getString("reference")).thenReturn("ref");
        when(rs.getString("status")).thenReturn("SENT");
        when(rs.getLong("version")).thenReturn(0L);
        when(rs.getString("payment_type")).thenReturn("BENEFICIARY_TRANSFER");
        when(rs.getString("payment_method")).thenReturn("NET_BANKING");
        when(rs.getString("card_type")).thenReturn(null);
        when(rs.getString("payer_id")).thenReturn("payer");
        when(rs.getString("invoice_id")).thenReturn(null);
        when(rs.getString("source_account_id")).thenReturn(null);
        when(rs.getString("beneficiary_id")).thenReturn("ben");
        when(rs.getString("card_last4")).thenReturn(null);
        when(rs.getString("card_holder_name")).thenReturn(null);
        when(rs.getString("upi_id")).thenReturn(null);
        when(rs.getString("idempotency_key")).thenReturn("key-1");
        when(rs.getTimestamp("created_at")).thenReturn(Timestamp.from(Instant.now()));
        when(rs.getTimestamp("updated_at")).thenReturn(Timestamp.from(Instant.now()));
        return rs;
    }

    @Test
    void findById_absent_returnsEmpty() {
        when(jdbc.query(anyString(), org.mockito.ArgumentMatchers.<RowMapper<Payment>>any(), eq("missing")))
                .thenReturn(List.of());

        assertFalse(repository.findById("missing").isPresent());
    }

    @Test
    void existsById_true() {
        when(jdbc.queryForObject(anyString(), eq(Integer.class), eq("100000001"))).thenReturn(1);

        assertTrue(repository.existsById("100000001"));
    }

    @Test
    void findByIdempotencyKey_present() throws Exception {
        ResultSet rs = mockPaymentRow();
        ArgumentCaptor<RowMapper<Payment>> captor = ArgumentCaptor.forClass(RowMapper.class);
        when(jdbc.query(anyString(), captor.capture(), eq("key-1"))).thenAnswer(inv ->
                List.of(captor.getValue().mapRow(rs, 0)));

        Optional<Payment> result = repository.findByIdempotencyKey("key-1");

        assertTrue(result.isPresent());
    }

    @Test
    void findByPayerIdAndInvoiceId_absent() {
        when(jdbc.query(anyString(), org.mockito.ArgumentMatchers.<RowMapper<Payment>>any(), eq("payer"), eq("INV-1")))
                .thenReturn(List.of());

        assertFalse(repository.findByPayerIdAndInvoiceId("payer", "INV-1").isPresent());
    }

    @Test
    void findAll_pageable_returnsPageWithTotalCount() throws Exception {
        when(jdbc.queryForObject(eq("SELECT COUNT(*) FROM payments"), eq(Integer.class))).thenReturn(25);
        ResultSet rs = mockPaymentRow();
        ArgumentCaptor<RowMapper<Payment>> captor = ArgumentCaptor.forClass(RowMapper.class);
        when(jdbc.query(anyString(), captor.capture(), any(Integer.class), any(Long.class))).thenAnswer(inv ->
                List.of(captor.getValue().mapRow(rs, 0)));

        Pageable pageable = PageRequest.of(0, 20, Sort.by(Sort.Order.desc("createdAt")));
        Page<Payment> page = repository.findAll(pageable);

        assertEquals(25, page.getTotalElements());
        assertEquals(1, page.getContent().size());
    }

    @Test
    void findAll_unsortedPageable_usesDefaultOrder() throws Exception {
        when(jdbc.queryForObject(eq("SELECT COUNT(*) FROM payments"), eq(Integer.class))).thenReturn(0);
        when(jdbc.query(anyString(), org.mockito.ArgumentMatchers.<RowMapper<Payment>>any(), any(Integer.class), any(Long.class)))
                .thenReturn(List.of());

        Pageable pageable = PageRequest.of(0, 20);
        Page<Payment> page = repository.findAll(pageable);

        assertEquals(0, page.getTotalElements());
    }

    @Test
    void findByStatus_returnsFilteredPage() throws Exception {
        when(jdbc.queryForObject(anyString(), eq(Integer.class), eq("SENT"))).thenReturn(30);
        ResultSet rs = mockPaymentRow();
        ArgumentCaptor<RowMapper<Payment>> captor = ArgumentCaptor.forClass(RowMapper.class);
        when(jdbc.query(anyString(), captor.capture(), eq("SENT"), any(Integer.class), any(Long.class)))
                .thenAnswer(inv -> List.of(captor.getValue().mapRow(rs, 0)));

        Pageable pageable = PageRequest.of(0, 20);
        Page<Payment> page = repository.findByStatus(PaymentStatus.SENT, pageable);

        assertEquals(30, page.getTotalElements());
    }
}
