package com.finance.PaymentProcessing.repository;

import com.finance.PaymentProcessing.model.Beneficiary;
import java.util.List;
import java.util.Optional;

public interface BeneficiaryRepository {
    Beneficiary save(Beneficiary beneficiary);
    Optional<Beneficiary> findById(String id);
    List<Beneficiary> findAll();
    boolean existsById(String id);
    Optional<Beneficiary> findByAccountNumber(String accountNumber);
}