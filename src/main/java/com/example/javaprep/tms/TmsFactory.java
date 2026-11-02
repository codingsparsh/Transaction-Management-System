package com.example.javaprep.tms;

import com.example.javaprep.tms.executor.DepositTransactionExecutor;
import com.example.javaprep.tms.executor.TransactionExecutor;
import com.example.javaprep.tms.executor.TransferTransactionExecutor;
import com.example.javaprep.tms.executor.WithdrawalTransactionExecutor;
import com.example.javaprep.tms.repository.AccountRepository;
import com.example.javaprep.tms.repository.InMemoryAccountRepository;
import com.example.javaprep.tms.repository.InMemoryTransactionRepository;
import com.example.javaprep.tms.repository.TransactionRepository;
import com.example.javaprep.tms.service.TransactionService;
import com.example.javaprep.tms.validator.BalanceSufficiencyValidator;
import com.example.javaprep.tms.validator.SelfTransferValidator;
import com.example.javaprep.tms.validator.TransactionLimitValidator;
import com.example.javaprep.tms.validator.TransactionValidator;
import com.example.javaprep.tms.validator.ValidationPipeline;

import java.math.BigDecimal;
import java.util.List;

public class TmsFactory {

    public static TransactionService createDefaultService(BigDecimal maxTransactionLimit) {
        AccountRepository accountRepo = new InMemoryAccountRepository();
        TransactionRepository txRepo = new InMemoryTransactionRepository();

        List<TransactionValidator> validators = List.of(
                new SelfTransferValidator(),
                new TransactionLimitValidator(maxTransactionLimit),
                new BalanceSufficiencyValidator()
        );
        ValidationPipeline pipeline = new ValidationPipeline(validators);

        List<TransactionExecutor> executors = List.of(
                new TransferTransactionExecutor(),
                new DepositTransactionExecutor(),
                new WithdrawalTransactionExecutor()
        );

        return new TransactionService(accountRepo, txRepo, pipeline, executors);
    }
}
