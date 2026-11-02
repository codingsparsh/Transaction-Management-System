package com.example.javaprep.tms.model;

import com.example.javaprep.tms.exception.InsufficientBalanceException;
import com.example.javaprep.tms.exception.ValidationException;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Objects;
import java.util.concurrent.locks.ReentrantLock;

public class Account {
    private final String id;
    private BigDecimal balance;
    private final ReentrantLock lock;

    public Account(String id, BigDecimal initialBalance) {
        this.id = Objects.requireNonNull(id, "Account ID cannot be null");
        if (initialBalance == null || initialBalance.compareTo(BigDecimal.ZERO) < 0) {
            throw new ValidationException("Initial balance cannot be null or negative");
        }
        this.balance = initialBalance.setScale(2, RoundingMode.HALF_UP);
        this.lock = new ReentrantLock();
    }

    public String getId() {
        return id;
    }

    public BigDecimal getBalance() {
        return balance;
    }

    public void debit(BigDecimal amount) {
        validateAmount(amount);
        if (this.balance.compareTo(amount) < 0) {
            throw new InsufficientBalanceException("Insufficient funds in account " + id + ". Available: " + balance + ", Requested: " + amount);
        }
        this.balance = this.balance.subtract(amount);
    }

    public void credit(BigDecimal amount) {
        validateAmount(amount);
        this.balance = this.balance.add(amount);
    }

    public void lock() {
        this.lock.lock();
    }

    public void unlock() {
        this.lock.unlock();
    }

    private void validateAmount(BigDecimal amount) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new ValidationException("Transaction amount must be strictly positive");
        }
    }
}
