package com.example.javaprep.tms.executor;

import com.example.javaprep.tms.exception.ValidationException;
import com.example.javaprep.tms.model.Account;
import com.example.javaprep.tms.model.Transaction;
import com.example.javaprep.tms.model.TransactionType;

public class TransferTransactionExecutor implements TransactionExecutor {

    @Override
    public boolean supports(TransactionType type) {
        return type == TransactionType.TRANSFER;
    }

    @Override
    public void execute(Transaction transaction, Account fromAccount, Account toAccount) {
        if (fromAccount == null || toAccount == null) {
            throw new ValidationException("Both source and destination accounts must be present for a transfer");
        }

        // Deterministic Lock Ordering by Account ID to prevent circular-wait deadlocks
        Account firstLock = fromAccount.getId().compareTo(toAccount.getId()) < 0 ? fromAccount : toAccount;
        Account secondLock = fromAccount.getId().compareTo(toAccount.getId()) < 0 ? toAccount : fromAccount;

        firstLock.lock();
        try {
            secondLock.lock();
            try {
                fromAccount.debit(transaction.getAmount());
                toAccount.credit(transaction.getAmount());
                transaction.markCompleted();
            } finally {
                secondLock.unlock();
            }
        } finally {
            firstLock.unlock();
        }
    }
}
