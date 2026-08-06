package com.finance.PaymentProcessing.service;

import com.finance.PaymentProcessing.dto.*;
import com.finance.PaymentProcessing.exception.BadRequestException;
import com.finance.PaymentProcessing.exception.NotFoundException;
import com.finance.PaymentProcessing.model.Beneficiary;
import com.finance.PaymentProcessing.repository.BeneficiaryRepository;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class BeneficiaryService {
    private final BeneficiaryRepository repository;

    public BeneficiaryService(BeneficiaryRepository repository) {
        this.repository = repository;
    }

    public BeneficiaryResponse addBeneficiary(BeneficiaryRequest request) {
        if (repository.findByAccountNumber(request.accountNumber()).isPresent())
            throw new BadRequestException("A beneficiary with this account number already exists");
        Beneficiary beneficiary = new Beneficiary();
        beneficiary.setName(request.name());
        beneficiary.setAccountNumber(request.accountNumber());
        beneficiary.setBankName(request.bankName());
        beneficiary.setIfscCode(request.ifscCode().toUpperCase());
        beneficiary.setEmail(request.email());
        beneficiary.setPhone(request.phone());
        return toResponse(repository.save(beneficiary));
    }

    @Transactional(readOnly = true)
    public List<BeneficiaryResponse> listBeneficiaries() {
        return repository.findAll().stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public BeneficiaryResponse getBeneficiary(String id) {
        return toResponse(find(id));
    }

    private Beneficiary find(String id) {
        return repository.findById(id).orElseThrow(() -> new NotFoundException("Beneficiary not found: " + id));
    }

    private BeneficiaryResponse toResponse(Beneficiary b) {
        return new BeneficiaryResponse(b.getBeneficiaryId(), b.getName(), b.getAccountNumber(), b.getBankName(),
                b.getIfscCode(), b.getEmail(), b.getPhone());
    }
}