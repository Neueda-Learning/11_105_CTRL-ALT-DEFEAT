package com.finance.PaymentProcessing.repository.impl;

import com.finance.PaymentProcessing.model.PaymentHistory;
import com.finance.PaymentProcessing.model.PaymentStatus;
import com.finance.PaymentProcessing.repository.PaymentHistoryRepository;
import com.finance.PaymentProcessing.util.UniqueIdGenerator;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

@Repository
public class PaymentHistoryRepositoryImpl implements PaymentHistoryRepository {

    private final JdbcTemplate jdbc;

    public PaymentHistoryRepositoryImpl(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    // RowMapper: converts a MySQL result row → PaymentHistory POJO
    private static final RowMapper<PaymentHistory> ROW_MAPPER = (rs, rowNum) -> map(rs);

    private static PaymentHistory map(ResultSet rs) throws SQLException {
        PaymentHistory h = new PaymentHistory();
        h.setHistoryId(rs.getString("history_id"));
        h.setPaymentId(rs.getString("payment_id"));
        String oldStatus = rs.getString("old_status");
        h.setOldStatus(oldStatus != null ? PaymentStatus.valueOf(oldStatus) : null);
        h.setNewStatus(PaymentStatus.valueOf(rs.getString("new_status")));
        h.setTimestamp(rs.getTimestamp("timestamp").toInstant());
        h.setRemarks(rs.getString("remarks"));
        h.setErrorCode(rs.getString("error_code"));
        h.setActor(rs.getString("actor"));
        return h;
    }

    @Override
    public void save(PaymentHistory history) {
        history.setHistoryId(UniqueIdGenerator.generate());
        history.setTimestamp(Instant.now());
        jdbc.update(
            "INSERT INTO payment_history (history_id, payment_id, old_status, new_status, timestamp, remarks, error_code, actor) VALUES (?, ?, ?, ?, ?, ?, ?, ?)",
            history.getHistoryId(),
            history.getPaymentId(),
            history.getOldStatus() != null ? history.getOldStatus().name() : null,
            history.getNewStatus().name(),
            Timestamp.from(history.getTimestamp()),
            history.getRemarks(),
            history.getErrorCode(),
            history.getActor()
        );
    }

    @Override
    public List<PaymentHistory> findByPaymentIdOrderByTimestampAsc(String paymentId) {
        return jdbc.query(
            "SELECT * FROM payment_history WHERE payment_id = ? ORDER BY timestamp ASC",
            ROW_MAPPER,
            paymentId
        );
    }
}
