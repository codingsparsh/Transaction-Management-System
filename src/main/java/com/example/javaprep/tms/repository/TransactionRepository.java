package com.example.javaprep.tms.repository;

import com.example.javaprep.tms.model.Transaction;

import java.util.List;
import java.util.Optional;

public interface TransactionRepository {
    void save(Transaction transaction);
    Optional<Transaction> findById(String id);
    List<Transaction> findByAccountId(String accountId);
}
