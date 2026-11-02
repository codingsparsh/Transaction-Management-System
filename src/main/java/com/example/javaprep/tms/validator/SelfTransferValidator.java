package com.example.javaprep.tms.validator;

import com.example.javaprep.tms.exception.ValidationException;
import com.example.javaprep.tms.model.Account;
import com.example.javaprep.tms.model.Transaction;
import com.example.javaprep.tms.model.TransactionType;

public class SelfTransferValidator implements TransactionValidator {

    @Override
    public void validate(Transaction transaction, Account fromAccount, Account toAccount) {
        if (transaction.getType() == TransactionType.TRANSFER) {
            if (transaction.getFromAccountId() != null &&
                transaction.getFromAccountId().equals(transaction.getToAccountId())) {
                throw new ValidationException("Source and destination accounts cannot be identical: "
                        + transaction.getFromAccountId());
            }
        }
    }
}
