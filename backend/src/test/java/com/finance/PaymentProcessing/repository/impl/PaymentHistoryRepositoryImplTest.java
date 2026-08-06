package com.finance.PaymentProcessing.repository.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.finance.PaymentProcessing.model.PaymentHistory;
import com.finance.PaymentProcessing.model.PaymentStatus;
import java.sql.ResultSet;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;

@ExtendWith(MockitoExtension.class)
class PaymentHistoryRepositoryImplTest {

    @Mock
    private JdbcTemplate jdbc;

    private PaymentHistoryRepositoryImpl repository;

    @BeforeEach
    void setUp() {
        repository = new PaymentHistoryRepositoryImpl(jdbc);
    }

    @Test
    void save_generatesIdAndTimestamp_thenInserts() {
        PaymentHistory history = new PaymentHistory();
        history.setPaymentId("100000001");
        history.setNewStatus(PaymentStatus.SENT);
        history.setActor("SYSTEM");

        repository.save(history);

        assertEquals(9, history.getHistoryId().length());
        verify(jdbc).update(anyString(), eq(history.getHistoryId()), eq("100000001"), isNull(),
                eq("SENT"), any(Timestamp.class), isNull(), isNull(), eq("SYSTEM"));
    }

    @Test
    void save_withOldStatusAndRemarks_insertsAllFields() {
        PaymentHistory history = new PaymentHistory();
        history.setPaymentId("100000001");
        history.setOldStatus(PaymentStatus.CREATED);
        history.setNewStatus(PaymentStatus.VALIDATED);
        history.setRemarks("remarks");
        history.setErrorCode("ERR");
        history.setActor("SYSTEM");

        repository.save(history);

        verify(jdbc).update(anyString(), eq(history.getHistoryId()), eq("100000001"), eq("CREATED"),
                eq("VALIDATED"), any(Timestamp.class), eq("remarks"), eq("ERR"), eq("SYSTEM"));
    }

    @Test
    void findByPaymentIdOrderByTimestampAsc_mapsRows() throws Exception {
        ResultSet rs = org.mockito.Mockito.mock(ResultSet.class);
        when(rs.getString("history_id")).thenReturn("1");
        when(rs.getString("payment_id")).thenReturn("100000001");
        when(rs.getString("old_status")).thenReturn("CREATED");
        when(rs.getString("new_status")).thenReturn("VALIDATED");
        when(rs.getTimestamp("timestamp")).thenReturn(Timestamp.from(Instant.now()));
        when(rs.getString("remarks")).thenReturn("remarks");
        when(rs.getString("error_code")).thenReturn(null);
        when(rs.getString("actor")).thenReturn("SYSTEM");

        ArgumentCaptor<RowMapper<PaymentHistory>> captor = ArgumentCaptor.forClass(RowMapper.class);
        when(jdbc.query(anyString(), captor.capture(), eq("100000001"))).thenAnswer(inv ->
                List.of(captor.getValue().mapRow(rs, 0)));

        List<PaymentHistory> result = repository.findByPaymentIdOrderByTimestampAsc("100000001");

        assertEquals(1, result.size());
        assertEquals(PaymentStatus.CREATED, result.get(0).getOldStatus());
        assertEquals(PaymentStatus.VALIDATED, result.get(0).getNewStatus());
    }

    @Test
    void findByPaymentIdOrderByTimestampAsc_nullOldStatus_mapsAsNull() throws Exception {
        ResultSet rs = org.mockito.Mockito.mock(ResultSet.class);
        when(rs.getString("history_id")).thenReturn("1");
        when(rs.getString("payment_id")).thenReturn("100000001");
        when(rs.getString("old_status")).thenReturn(null);
        when(rs.getString("new_status")).thenReturn("SENT");
        when(rs.getTimestamp("timestamp")).thenReturn(Timestamp.from(Instant.now()));

        ArgumentCaptor<RowMapper<PaymentHistory>> captor = ArgumentCaptor.forClass(RowMapper.class);
        when(jdbc.query(anyString(), captor.capture(), eq("100000001"))).thenAnswer(inv ->
                List.of(captor.getValue().mapRow(rs, 0)));

        List<PaymentHistory> result = repository.findByPaymentIdOrderByTimestampAsc("100000001");

        assertEquals(null, result.get(0).getOldStatus());
    }
}
