package com.example.javaprep.tms.executor;

import com.example.javaprep.tms.exception.ValidationException;
import com.example.javaprep.tms.model.Account;
import com.example.javaprep.tms.model.Transaction;
import com.example.javaprep.tms.model.TransactionType;

public class WithdrawalTransactionExecutor implements TransactionExecutor {

    @Override
    public boolean supports(TransactionType type) {
        return type == TransactionType.WITHDRAWAL;
    }

    @Override
    public void execute(Transaction transaction, Account fromAccount, Account toAccount) {
        if (fromAccount == null) {
            throw new ValidationException("Source account cannot be null for withdrawal");
        }
        fromAccount.lock();
        try {
            fromAccount.debit(transaction.getAmount());
            transaction.markCompleted();
        } finally {
            fromAccount.unlock();
        }
    }
}
