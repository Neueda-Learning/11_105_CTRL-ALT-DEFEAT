package com.finance.PaymentProcessing.repository.impl;

import com.finance.PaymentProcessing.model.Beneficiary;
import com.finance.PaymentProcessing.repository.BeneficiaryRepository;
import com.finance.PaymentProcessing.util.UniqueIdGenerator;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

@Repository
public class BeneficiaryRepositoryImpl implements BeneficiaryRepository {

    private final JdbcTemplate jdbc;

    public BeneficiaryRepositoryImpl(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    // RowMapper: converts a MySQL result row → Beneficiary POJO
    private static final RowMapper<Beneficiary> ROW_MAPPER = (rs, rowNum) -> map(rs);

    private static Beneficiary map(ResultSet rs) throws SQLException {
        Beneficiary b = new Beneficiary();
        b.setBeneficiaryId(rs.getString("beneficiary_id"));
        b.setName(rs.getString("name"));
        b.setAccountNumber(rs.getString("account_number"));
        b.setBankName(rs.getString("bank_name"));
        b.setIfscCode(rs.getString("ifsc_code"));
        b.setEmail(rs.getString("email"));
        b.setPhone(rs.getString("phone"));
        return b;
    }

    @Override
    public Beneficiary save(Beneficiary beneficiary) {
        if (beneficiary.getBeneficiaryId() == null) {
            beneficiary.setBeneficiaryId(UniqueIdGenerator.generate());
            jdbc.update(
                "INSERT INTO beneficiaries (beneficiary_id, name, account_number, bank_name, ifsc_code, email, phone) VALUES (?, ?, ?, ?, ?, ?, ?)",
                beneficiary.getBeneficiaryId(),
                beneficiary.getName(),
                beneficiary.getAccountNumber(),
                beneficiary.getBankName(),
                beneficiary.getIfscCode(),
                beneficiary.getEmail(),
                beneficiary.getPhone()
            );
        } else {
            jdbc.update(
                "UPDATE beneficiaries SET name = ?, account_number = ?, bank_name = ?, ifsc_code = ?, email = ?, phone = ? WHERE beneficiary_id = ?",
                beneficiary.getName(),
                beneficiary.getAccountNumber(),
                beneficiary.getBankName(),
                beneficiary.getIfscCode(),
                beneficiary.getEmail(),
                beneficiary.getPhone(),
                beneficiary.getBeneficiaryId()
            );
        }
        return beneficiary;
    }

    @Override
    public Optional<Beneficiary> findById(String id) {
        List<Beneficiary> results = jdbc.query(
            "SELECT * FROM beneficiaries WHERE beneficiary_id = ?",
            ROW_MAPPER,
            id
        );
        return results.isEmpty() ? Optional.empty() : Optional.of(results.get(0));
    }

    @Override
    public List<Beneficiary> findAll() {
        return jdbc.query("SELECT * FROM beneficiaries", ROW_MAPPER);
    }

    @Override
    public boolean existsById(String id) {
        Integer count = jdbc.queryForObject(
            "SELECT COUNT(*) FROM beneficiaries WHERE beneficiary_id = ?",
            Integer.class,
            id
        );
        return count != null && count > 0;
    }

    @Override
    public Optional<Beneficiary> findByAccountNumber(String accountNumber) {
        List<Beneficiary> results = jdbc.query(
            "SELECT * FROM beneficiaries WHERE account_number = ?",
            ROW_MAPPER,
            accountNumber
        );
        return results.isEmpty() ? Optional.empty() : Optional.of(results.get(0));
    }
}