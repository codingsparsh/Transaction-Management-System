package com.example.javaprep.tms.repository;

import com.example.javaprep.tms.model.Transaction;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

public class InMemoryTransactionRepository implements TransactionRepository {
    // Primary index: transactionId -> Transaction
    private final Map<String, Transaction> transactions = new ConcurrentHashMap<>();

    // Secondary index: accountId -> List<Transaction> for O(1) passbook retrieval
    private final Map<String, List<Transaction>> accountIndex = new ConcurrentHashMap<>();

    @Override
    public void save(Transaction transaction) {
        boolean isNew = (transactions.put(transaction.getId(), transaction) == null);

        if (isNew) {
            if (transaction.getFromAccountId() != null) {
                accountIndex.computeIfAbsent(transaction.getFromAccountId(), k -> new CopyOnWriteArrayList<>())
                        .add(transaction);
            }
            if (transaction.getToAccountId() != null &&
                    !transaction.getToAccountId().equals(transaction.getFromAccountId())) {
                accountIndex.computeIfAbsent(transaction.getToAccountId(), k -> new CopyOnWriteArrayList<>())
                        .add(transaction);
            }
        }
    }

    @Override
    public Optional<Transaction> findById(String id) {
        return Optional.ofNullable(transactions.get(id));
    }

    @Override
    public List<Transaction> findByAccountId(String accountId) {
        List<Transaction> list = accountIndex.get(accountId);
        if (list == null) {
            return Collections.emptyList();
        }
        return Collections.unmodifiableList(list);
    }
}
