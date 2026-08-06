package com.finance.PaymentProcessing.model;

public class BankAccount {
    private String accountId;
    private String accountNumber;
    private String accountHolderName;
    private boolean active = true;

    public String getAccountId() { return accountId; }
    public void setAccountId(String accountId) { this.accountId = accountId; }
    public String getAccountNumber() { return accountNumber; }
    public void setAccountNumber(String accountNumber) { this.accountNumber = accountNumber; }
    public String getAccountHolderName() { return accountHolderName; }
    public void setAccountHolderName(String accountHolderName) { this.accountHolderName = accountHolderName; }
    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }
}