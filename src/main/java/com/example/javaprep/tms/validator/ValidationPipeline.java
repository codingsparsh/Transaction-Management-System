package com.example.javaprep.tms.validator;

import com.example.javaprep.tms.model.Account;
import com.example.javaprep.tms.model.Transaction;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class ValidationPipeline {
    private final List<TransactionValidator> validators;

    public ValidationPipeline(List<TransactionValidator> validators) {
        this.validators = new ArrayList<>(validators);
    }

    public void validate(Transaction transaction, Account fromAccount, Account toAccount) {
        for (TransactionValidator validator : validators) {
            validator.validate(transaction, fromAccount, toAccount);
        }
    }

    public List<TransactionValidator> getValidators() {
        return Collections.unmodifiableList(validators);
    }
}
