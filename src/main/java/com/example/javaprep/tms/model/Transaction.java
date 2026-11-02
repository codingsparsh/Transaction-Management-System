package com.example.javaprep.tms.model;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Objects;

public class Transaction {
    private final String id;
    private final TransactionType type;
    private final String fromAccountId;
    private final String toAccountId;
    private final BigDecimal amount;
    private final Instant createdAt;
    
    private TransactionStatus status;
    private Instant updatedAt;
    private String failureReason;

    public Transaction(String id, TransactionType type, String fromAccountId, String toAccountId, BigDecimal amount) {
        this.id = Objects.requireNonNull(id, "Transaction ID cannot be null");
        this.type = Objects.requireNonNull(type, "Transaction type cannot be null");
        this.amount = Objects.requireNonNull(amount, "Amount cannot be null");
        this.fromAccountId = fromAccountId;
        this.toAccountId = toAccountId;
        this.status = TransactionStatus.PENDING;
        this.createdAt = Instant.now();
        this.updatedAt = this.createdAt;
    }

    public void markCompleted() {
        this.status = TransactionStatus.COMPLETED;
        this.updatedAt = Instant.now();
    }

    public void markFailed(String reason) {
        this.status = TransactionStatus.FAILED;
        this.failureReason = reason;
        this.updatedAt = Instant.now();
    }

    public String getId() {
        return id;
    }

    public TransactionType getType() {
        return type;
    }

    public String getFromAccountId() {
        return fromAccountId;
    }

    public String getToAccountId() {
        return toAccountId;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public TransactionStatus getStatus() {
        return status;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public String getFailureReason() {
        return failureReason;
    }
}
