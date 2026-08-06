package com.finance.PaymentProcessing.dto;

public record BeneficiaryResponse(String beneficiaryId, String name, String accountNumber, String bankName,
        String ifscCode, String email, String phone) {
}