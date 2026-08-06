package com.finance.PaymentProcessing.repository;

import com.finance.PaymentProcessing.model.BankAccount;
import java.util.List;
import java.util.Optional;

public interface BankAccountRepository {
    BankAccount save(BankAccount account);
    Optional<BankAccount> findById(String id);
    List<BankAccount> findAll();
    boolean existsById(String id);
}
