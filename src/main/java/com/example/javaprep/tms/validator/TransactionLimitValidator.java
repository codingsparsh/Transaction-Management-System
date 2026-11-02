package com.example.javaprep.tms.validator;

import com.example.javaprep.tms.exception.ValidationException;
import com.example.javaprep.tms.model.Account;
import com.example.javaprep.tms.model.Transaction;

import java.math.BigDecimal;

public class TransactionLimitValidator implements TransactionValidator {
    private final BigDecimal maxTransactionLimit;

    public TransactionLimitValidator(BigDecimal maxTransactionLimit) {
        if (maxTransactionLimit == null || maxTransactionLimit.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Max transaction limit must be positive");
        }
        this.maxTransactionLimit = maxTransactionLimit;
    }

    @Override
    public void validate(Transaction transaction, Account fromAccount, Account toAccount) {
        if (transaction.getAmount().compareTo(maxTransactionLimit) > 0) {
            throw new ValidationException("Transaction amount " + transaction.getAmount()
                    + " exceeds maximum allowed limit of " + maxTransactionLimit);
        }
    }
}
