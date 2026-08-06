package com.finance.PaymentProcessing.dto;
public record BankAccountResponse(String accountId, String accountNumber, String accountHolderName, boolean active) { }