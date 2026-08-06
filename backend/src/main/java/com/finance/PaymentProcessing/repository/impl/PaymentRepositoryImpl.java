package com.finance.PaymentProcessing.repository.impl;

import com.finance.PaymentProcessing.exception.ConflictException;
import com.finance.PaymentProcessing.model.CardType;
import com.finance.PaymentProcessing.model.Payment;
import com.finance.PaymentProcessing.model.PaymentMethod;
import com.finance.PaymentProcessing.model.PaymentStatus;
import com.finance.PaymentProcessing.model.PaymentType;
import com.finance.PaymentProcessing.repository.PaymentRepository;
import com.finance.PaymentProcessing.util.UniqueIdGenerator;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.StringJoiner;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

@Repository
public class PaymentRepositoryImpl implements PaymentRepository {

    private final JdbcTemplate jdbc;

    public PaymentRepositoryImpl(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    // Maps SQL column names to Java property names for ORDER BY translation
    private static final Map<String, String> SORT_COLUMN_MAP = Map.of(
        "createdAt",  "created_at",
        "updatedAt",  "updated_at",
        "status",     "status",
        "amount",     "amount",
        "currency",   "currency"
    );

    // RowMapper: converts each MySQL result row → Payment POJO
    private static final RowMapper<Payment> ROW_MAPPER = (rs, rowNum) -> map(rs);

    private static Payment map(ResultSet rs) throws SQLException {
        Payment p = new Payment();
        p.setPaymentId(rs.getString("payment_id"));
        p.setAmount(rs.getBigDecimal("amount"));
        p.setCurrency(rs.getString("currency"));
        p.setReference(rs.getString("reference"));
        p.setStatus(PaymentStatus.valueOf(rs.getString("status")));
        p.setVersion(rs.getLong("version"));
        p.setPaymentType(PaymentType.valueOf(rs.getString("payment_type")));
        p.setPaymentMethod(PaymentMethod.valueOf(rs.getString("payment_method")));
        String cardType = rs.getString("card_type");
        p.setCardType(cardType != null ? CardType.valueOf(cardType) : null);
        p.setPayerId(rs.getString("payer_id"));
        p.setInvoiceId(rs.getString("invoice_id"));
        p.setSourceAccountId(rs.getString("source_account_id"));
        p.setBeneficiaryId(rs.getString("beneficiary_id"));
        p.setCardLast4(rs.getString("card_last4"));
        p.setCardHolderName(rs.getString("card_holder_name"));
        p.setUpiId(rs.getString("upi_id"));
        p.setIdempotencyKey(rs.getString("idempotency_key"));
        p.setCreatedAt(rs.getTimestamp("created_at").toInstant());
        p.setUpdatedAt(rs.getTimestamp("updated_at").toInstant());
        return p;
    }

    @Override
    public Payment save(Payment payment) {
        if (payment.getPaymentId() == null) {
            // INSERT: assign a unique 9-digit id and timestamps
            payment.setPaymentId(UniqueIdGenerator.generate());
            Instant now = Instant.now();
            payment.setCreatedAt(now);
            payment.setUpdatedAt(now);
            payment.setVersion(0L);
            jdbc.update(
                "INSERT INTO payments (payment_id, amount, currency, reference, status, version, payment_type, payment_method, card_type, payer_id, invoice_id, source_account_id, beneficiary_id, card_last4, card_holder_name, upi_id, idempotency_key, created_at, updated_at) "
                + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
                payment.getPaymentId(),
                payment.getAmount(),
                payment.getCurrency(),
                payment.getReference(),
                payment.getStatus().name(),
                payment.getVersion(),
                payment.getPaymentType().name(),
                payment.getPaymentMethod().name(),
                payment.getCardType() != null ? payment.getCardType().name() : null,
                payment.getPayerId(),
                payment.getInvoiceId(),
                payment.getSourceAccountId(),
                payment.getBeneficiaryId(),
                payment.getCardLast4(),
                payment.getCardHolderName(),
                payment.getUpiId(),
                payment.getIdempotencyKey(),
                Timestamp.from(payment.getCreatedAt()),
                Timestamp.from(payment.getUpdatedAt())
            );
        } else {
            // UPDATE with optimistic version check: WHERE version = currentVersion
            long currentVersion = payment.getVersion() != null ? payment.getVersion() : 0L;
            long nextVersion = currentVersion + 1;
            Instant now = Instant.now();
            int updated = jdbc.update(
                "UPDATE payments SET amount = ?, currency = ?, reference = ?, status = ?, version = ?, payment_type = ?, payment_method = ?, card_type = ?, payer_id = ?, invoice_id = ?, source_account_id = ?, beneficiary_id = ?, card_last4 = ?, card_holder_name = ?, upi_id = ?, idempotency_key = ?, updated_at = ? "
                + "WHERE payment_id = ? AND version = ?",
                payment.getAmount(),
                payment.getCurrency(),
                payment.getReference(),
                payment.getStatus().name(),
                nextVersion,
                payment.getPaymentType().name(),
                payment.getPaymentMethod().name(),
                payment.getCardType() != null ? payment.getCardType().name() : null,
                payment.getPayerId(),
                payment.getInvoiceId(),
                payment.getSourceAccountId(),
                payment.getBeneficiaryId(),
                payment.getCardLast4(),
                payment.getCardHolderName(),
                payment.getUpiId(),
                payment.getIdempotencyKey(),
                Timestamp.from(now),
                payment.getPaymentId(),
                currentVersion
            );
            if (updated == 0) {
                throw new ConflictException("OPTIMISTIC_LOCK", "Payment was modified by another request; please retry");
            }
            payment.setVersion(nextVersion);
            payment.setUpdatedAt(now);
        }
        return payment;
    }

    @Override
    public Optional<Payment> findById(String id) {
        List<Payment> results = jdbc.query(
            "SELECT * FROM payments WHERE payment_id = ?",
            ROW_MAPPER,
            id
        );
        return results.isEmpty() ? Optional.empty() : Optional.of(results.get(0));
    }

    @Override
    public boolean existsById(String id) {
        Integer count = jdbc.queryForObject(
            "SELECT COUNT(*) FROM payments WHERE payment_id = ?",
            Integer.class,
            id
        );
        return count != null && count > 0;
    }

    @Override
    public Optional<Payment> findByIdempotencyKey(String key) {
        List<Payment> results = jdbc.query(
            "SELECT * FROM payments WHERE idempotency_key = ?",
            ROW_MAPPER,
            key
        );
        return results.isEmpty() ? Optional.empty() : Optional.of(results.get(0));
    }

    @Override
    public Optional<Payment> findByPayerIdAndInvoiceId(String payerId, String invoiceId) {
        List<Payment> results = jdbc.query(
            "SELECT * FROM payments WHERE payer_id = ? AND invoice_id = ?",
            ROW_MAPPER,
            payerId,
            invoiceId
        );
        return results.isEmpty() ? Optional.empty() : Optional.of(results.get(0));
    }

    @Override
    public Page<Payment> findAll(Pageable pageable) {
        int total = countAll(null);
        String order = toOrderClause(pageable.getSort());
        List<Payment> rows = jdbc.query(
            "SELECT * FROM payments " + order + " LIMIT ? OFFSET ?",
            ROW_MAPPER,
            pageable.getPageSize(),
            pageable.getOffset()
        );
        return new PageImpl<>(rows, pageable, total);
    }

    @Override
    public Page<Payment> findByStatus(PaymentStatus status, Pageable pageable) {
        int total = countAll(status);
        String order = toOrderClause(pageable.getSort());
        List<Payment> rows = jdbc.query(
            "SELECT * FROM payments WHERE status = ? " + order + " LIMIT ? OFFSET ?",
            ROW_MAPPER,
            status.name(),
            pageable.getPageSize(),
            pageable.getOffset()
        );
        return new PageImpl<>(rows, pageable, total);
    }

    private int countAll(PaymentStatus status) {
        Integer count;
        if (status == null) {
            count = jdbc.queryForObject("SELECT COUNT(*) FROM payments", Integer.class);
        } else {
            count = jdbc.queryForObject(
                "SELECT COUNT(*) FROM payments WHERE status = ?",
                Integer.class,
                status.name()
            );
        }
        return count != null ? count : 0;
    }

    // Translates Spring Data Sort into a safe SQL ORDER BY clause
    private String toOrderClause(Sort sort) {
        if (sort == null || sort.isUnsorted()) {
            return "ORDER BY created_at DESC";
        }
        StringJoiner joiner = new StringJoiner(", ", "ORDER BY ", "");
        for (Sort.Order order : sort) {
            String col = SORT_COLUMN_MAP.getOrDefault(order.getProperty(), "created_at");
            joiner.add(col + " " + order.getDirection().name());
        }
        return joiner.toString();
    }
}
