package com.finance.PaymentProcessing.repository.impl;

import com.finance.PaymentProcessing.model.BankAccount;
import com.finance.PaymentProcessing.repository.BankAccountRepository;
import com.finance.PaymentProcessing.util.UniqueIdGenerator;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

@Repository
public class BankAccountRepositoryImpl implements BankAccountRepository {

    private final JdbcTemplate jdbc;

    public BankAccountRepositoryImpl(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    // RowMapper: converts a MySQL result row → BankAccount POJO
    private static final RowMapper<BankAccount> ROW_MAPPER = (rs, rowNum) -> map(rs);

    private static BankAccount map(ResultSet rs) throws SQLException {
        BankAccount a = new BankAccount();
        a.setAccountId(rs.getString("account_id"));
        a.setAccountNumber(rs.getString("account_number"));
        a.setAccountHolderName(rs.getString("account_holder_name"));
        a.setActive(rs.getBoolean("active"));
        return a;
    }

    @Override
    public BankAccount save(BankAccount account) {
        if (account.getAccountId() == null) {
            // INSERT: generate a unique 9-digit id here so MySQL receives it as a CHAR(9)
            account.setAccountId(UniqueIdGenerator.generate());
            jdbc.update(
                "INSERT INTO bank_accounts (account_id, account_number, account_holder_name, active) VALUES (?, ?, ?, ?)",
                account.getAccountId(),
                account.getAccountNumber(),
                account.getAccountHolderName(),
                account.isActive()
            );
        } else {
            // UPDATE
            jdbc.update(
                "UPDATE bank_accounts SET account_number = ?, account_holder_name = ?, active = ? WHERE account_id = ?",
                account.getAccountNumber(),
                account.getAccountHolderName(),
                account.isActive(),
                account.getAccountId()
            );
        }
        return account;
    }

    @Override
    public Optional<BankAccount> findById(String id) {
        List<BankAccount> results = jdbc.query(
            "SELECT * FROM bank_accounts WHERE account_id = ?",
            ROW_MAPPER,
            id
        );
        return results.isEmpty() ? Optional.empty() : Optional.of(results.get(0));
    }

    @Override
    public List<BankAccount> findAll() {
        return jdbc.query("SELECT * FROM bank_accounts", ROW_MAPPER);
    }

    @Override
    public boolean existsById(String id) {
        Integer count = jdbc.queryForObject(
            "SELECT COUNT(*) FROM bank_accounts WHERE account_id = ?",
            Integer.class,
            id
        );
        return count != null && count > 0;
    }
}