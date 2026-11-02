package com.example.javaprep.tms.executor;

import com.example.javaprep.tms.exception.ValidationException;
import com.example.javaprep.tms.model.Account;
import com.example.javaprep.tms.model.Transaction;
import com.example.javaprep.tms.model.TransactionType;

public class DepositTransactionExecutor implements TransactionExecutor {

    @Override
    public boolean supports(TransactionType type) {
        return type == TransactionType.DEPOSIT;
    }

    @Override
    public void execute(Transaction transaction, Account fromAccount, Account toAccount) {
        if (toAccount == null) {
            throw new ValidationException("Destination account cannot be null for deposit");
        }
        toAccount.lock();
        try {
            toAccount.credit(transaction.getAmount());
            transaction.markCompleted();
        } finally {
            toAccount.unlock();
        }
    }
}
