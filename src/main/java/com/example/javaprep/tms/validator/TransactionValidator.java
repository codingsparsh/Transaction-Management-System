package com.example.javaprep.tms.validator;

import com.example.javaprep.tms.model.Account;
import com.example.javaprep.tms.model.Transaction;

public interface TransactionValidator {
    void validate(Transaction transaction, Account fromAccount, Account toAccount);
}
