package com.example.javaprep.tms.executor;

import com.example.javaprep.tms.model.Account;
import com.example.javaprep.tms.model.Transaction;
import com.example.javaprep.tms.model.TransactionType;

public interface TransactionExecutor {
    boolean supports(TransactionType type);
    void execute(Transaction transaction, Account fromAccount, Account toAccount);
}
