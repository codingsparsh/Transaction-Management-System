package com.example.javaprep.tms.validator;

import com.example.javaprep.tms.exception.InsufficientBalanceException;
import com.example.javaprep.tms.model.Account;
import com.example.javaprep.tms.model.Transaction;
import com.example.javaprep.tms.model.TransactionType;

public class BalanceSufficiencyValidator implements TransactionValidator {

    @Override
    public void validate(Transaction transaction, Account fromAccount, Account toAccount) {
        if (transaction.getType() == TransactionType.TRANSFER || transaction.getType() == TransactionType.WITHDRAWAL) {
            if (fromAccount == null) {
                return;
            }
            if (fromAccount.getBalance().compareTo(transaction.getAmount()) < 0) {
                throw new InsufficientBalanceException("Insufficient balance in account: " + fromAccount.getId()
                        + ". Available: " + fromAccount.getBalance()
                        + ", Requested: " + transaction.getAmount());
            }
        }
    }
}
