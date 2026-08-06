package com.finance.PaymentProcessing.model;

import java.time.Instant;

public class PaymentHistory {
    private String historyId;
    private String paymentId;
    private PaymentStatus oldStatus;
    private PaymentStatus newStatus;
    private Instant timestamp;
    private String remarks;
    private String errorCode;
    private String actor;

    public String getHistoryId() { return historyId; }
    public void setHistoryId(String historyId) { this.historyId = historyId; }
    public String getPaymentId() { return paymentId; }
    public void setPaymentId(String paymentId) { this.paymentId = paymentId; }
    public PaymentStatus getOldStatus() { return oldStatus; }
    public void setOldStatus(PaymentStatus oldStatus) { this.oldStatus = oldStatus; }
    public PaymentStatus getNewStatus() { return newStatus; }
    public void setNewStatus(PaymentStatus newStatus) { this.newStatus = newStatus; }
    public Instant getTimestamp() { return timestamp; }
    public void setTimestamp(Instant timestamp) { this.timestamp = timestamp; }
    public String getRemarks() { return remarks; }
    public void setRemarks(String remarks) { this.remarks = remarks; }
    public String getErrorCode() { return errorCode; }
    public void setErrorCode(String errorCode) { this.errorCode = errorCode; }
    public String getActor() { return actor; }
    public void setActor(String actor) { this.actor = actor; }
}
